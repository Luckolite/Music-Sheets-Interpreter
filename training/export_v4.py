# Copyright 2026 Luckolite
# SPDX-License-Identifier: Apache-2.0
"""Export six-class segmentation labels and optional auxiliary centre heatmaps."""
from pathlib import Path
import os,argparse,json,hashlib,time
os.environ.setdefault('CUDA_VISIBLE_DEVICES','-1')
os.environ.setdefault('TF_CPP_MIN_LOG_LEVEL','2')
os.environ.setdefault('TF_ENABLE_ONEDNN_OPTS','0')
import numpy as np
import tensorflow as tf

def main():
 ap=argparse.ArgumentParser();ap.add_argument('weights',type=Path);ap.add_argument('out',type=Path)
 args=ap.parse_args();args.out.mkdir(parents=True,exist_ok=True)
 tf.config.set_visible_devices([], 'GPU');tf.config.threading.set_intra_op_parallelism_threads(2);tf.config.threading.set_inter_op_parallelism_threads(2)
 data=np.load(args.weights,allow_pickle=False)
 architecture=json.loads(str(data['architecture_json'].item())) if 'architecture_json' in data else dict(architecture='musicsheets-tiny-unet-v1-plus-1x1-centres',widths=[16,32,64,128],input_size=320,dilations={})
 widths=architecture['widths'];dilations=architecture.get('dilations',{})
 assert len(widths)==4 and all(isinstance(n,int) and n>0 for n in widths)
 assert architecture['input_size']==320,'This exporter preserves the native320tile contract'
 assert isinstance(dilations,dict) and set(dilations)<=set(n+'.'+i for n in 'abcduvw' for i in ['0','2'])
 assert all(isinstance(d,int) and 1<=d<=8 for d in dilations.values())
 names=[n for n in ['centres','centers','centre','center'] if n+'.weight' in data]
 assert len(names)==1,'Expected one 1x1 centre head';centre_name=names[0]
 assert data[centre_name+'.weight'].shape==(1,widths[0],1,1)
 assert data['out.weight'].shape==(6,widths[0],1,1)
 for name,cin,cout in [('a',1,widths[0]),('b',widths[0],widths[1]),('c',widths[1],widths[2]),('d',widths[2],widths[3]),('u',widths[3]+widths[2],widths[2]),('v',widths[2]+widths[1],widths[1]),('w',widths[1]+widths[0],widths[0])]:
  assert data[name+'.0.weight'].shape==(cout,cin,3,3),(name,'input channels')
  assert data[name+'.2.weight'].shape==(cout,cout,3,3),(name,'output channels')
 def conv(x,name):
  weight=data[name+'.weight'];assert weight.shape[2]==weight.shape[3]
  layer=tf.keras.layers.Conv2D(weight.shape[0],weight.shape[2],padding='same',dilation_rate=dilations.get(name,1),name=name.replace('.','_'))
  result=layer(x);layer.set_weights([weight.transpose(2,3,1,0),data[name+'.bias']]);return result
 def block(x,name):return tf.nn.relu(conv(tf.nn.relu(conv(x,name+'.0')),name+'.2'))
 inp=tf.keras.Input(shape=(3,320,320),batch_size=1,dtype=tf.float32,name='raw_rgb')
 a=block(tf.transpose(inp[:,:1]/255.,[0,2,3,1]),'a');b=block(tf.nn.max_pool2d(a,2,2,'VALID'),'b')
 c=block(tf.nn.max_pool2d(b,2,2,'VALID'),'c');d=block(tf.nn.max_pool2d(c,2,2,'VALID'),'d')
 def up(x,skip,name):return block(tf.concat([tf.keras.layers.UpSampling2D(size=2,interpolation='nearest')(x),skip],-1),name)
 features=up(up(up(d,c,'u'),b,'v'),a,'w');semantic=conv(features,'out');centres=conv(features,centre_name)
 keras=tf.keras.Model(inp,[semantic,centres]);samples=data['sample_inputs'];expected=data['sample_logits'];expected_c=data['sample_center_logits']
 assert samples.ndim==4 and samples.shape[1:]==(3,320,320)
 assert expected.shape==(len(samples),6,320,320) and expected_c.shape==(len(samples),1,320,320)
 actual=[];actual_c=[]
 for sample in samples:
  s,c=keras(sample[None],training=False);actual.append(s.numpy().transpose(0,3,1,2));actual_c.append(c.numpy().transpose(0,3,1,2))
 actual=np.concatenate(actual);actual_c=np.concatenate(actual_c)
 assert np.allclose(actual,expected,rtol=.0002,atol=.0001),float(np.abs(actual-expected).max())
 assert np.allclose(actual_c,expected_c,rtol=.0002,atol=.0001),float(np.abs(actual_c-expected_c).max())
 labels=tf.argmax(semantic,-1,output_type=tf.int64);probability=tf.sigmoid(centres)[...,0]
 legacy=tf.keras.Model(inp,labels);joint_keras=tf.keras.Model(inp,[labels,probability])
 class Joint(tf.Module):
  def __init__(self):super().__init__();self.network=joint_keras
  @tf.function(input_signature=[tf.TensorSpec([1,3,320,320],tf.float32,name='raw_rgb')])
  def serve(self,raw_rgb):
   labels,centres=self.network(raw_rgb,training=False)
   return {'labels':labels,'centres':centres}
 joint=Joint();concrete=joint.serve.get_concrete_function()
 report=dict(architecture=architecture['architecture'],architecture_config=architecture,weights_sha256=hashlib.sha256(args.weights.read_bytes()).hexdigest(),
  exporter_sha256=hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),tensorflow=tf.__version__,cpu_only=True,
  torch_keras_logit_error=dict(semantic=float(np.abs(actual-expected).max()),centres=float(np.abs(actual_c-expected_c).max())),
  quality_approved=False,production_install_allowed=False,scope='Numerical conversion and tensor-contract checks only; no recognition-quality claim.',models={})
 expected_prob=1/(1+np.exp(-np.clip(expected_c[:,0],-80,80)))
 for mode in ['float32','float16']:
  for kind in ['semantic','joint']:
   converter=tf.lite.TFLiteConverter.from_keras_model(legacy) if kind=='semantic' else tf.lite.TFLiteConverter.from_concrete_functions([concrete],joint)
   if mode=='float16':converter.optimizations=[tf.lite.Optimize.DEFAULT];converter.target_spec.supported_types=[tf.float16]
   payload=converter.convert();stem=mode if kind=='semantic' else 'joint-'+mode;path=args.out/(stem+'.tflite');path.write_bytes(payload)
   runtime=tf.lite.Interpreter(model_path=str(path),num_threads=2);runtime.allocate_tensors();inputs=runtime.get_input_details();outputs=runtime.get_output_details()
   assert len(inputs)==1 and inputs[0]['shape'].tolist()==[1,3,320,320] and inputs[0]['dtype']==np.float32
   signatures=runtime.get_signature_list();runner=runtime.get_signature_runner('serving_default') if kind=='joint' else None
   if kind=='joint':assert set(signatures['serving_default']['outputs'])=={'labels','centres'}
   else:assert len(outputs)==1 and outputs[0]['shape'].tolist()==[1,320,320] and outputs[0]['dtype']==np.int64
   agreements=[];errors=[];seconds=[]
   for i,sample in enumerate(samples):
    started=time.perf_counter()
    if runner:result=runner(raw_rgb=sample[None].astype(np.float32));out=result['labels'];heatmap=result['centres']
    else:
     runtime.set_tensor(inputs[0]['index'],sample[None].astype(np.float32));runtime.invoke();out=runtime.get_tensor(outputs[0]['index'])
    seconds.append(time.perf_counter()-started);assert out.shape==(1,320,320) and out.dtype==np.int64
    agreements.append(float(np.mean(out[0]==expected[i].argmax(0))))
    if runner:
     assert heatmap.shape==(1,320,320) and heatmap.dtype==np.float32 and np.isfinite(heatmap).all()
     assert heatmap.min()>=0 and heatmap.max()<=1
     errors.append(float(np.abs(heatmap[0]-expected_prob[i]).max()))
   if mode=='float32':
    assert min(agreements)>.9999,agreements
    if errors:assert max(errors)<.0001,errors
   operators=sorted({op['op_name'] for op in runtime._get_ops_details()});assert not any(op.startswith('Flex') for op in operators)
   report['models'][stem]=dict(bytes=len(payload),sha256=hashlib.sha256(payload).hexdigest(),signatures=signatures,
    outputs=[dict(name=v['name'],index=int(v['index']),shape=v['shape'].tolist(),dtype=np.dtype(v['dtype']).name) for v in outputs],
    semantic_agreement=agreements,centre_probability_max_errors=errors,cpu_tile_seconds=seconds,operators=operators)
   print(stem,'semantic minimum agreement',min(agreements),'max centre error',max(errors) if errors else None,flush=True)
 (args.out/'export.json').write_text(json.dumps(report,indent=2)+'\n')
if __name__=='__main__':main()
