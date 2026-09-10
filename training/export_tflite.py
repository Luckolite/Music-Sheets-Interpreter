# Copyright 2026 Luckolite
# SPDX-License-Identifier: Apache-2.0
"""Mirror our own PyTorch graph in TensorFlow and verify conversion numerically.

Run in .venv-export (TensorFlow 2.15.1). Inputs are our generated .npz tensors.
"""
import argparse, json, os, hashlib, time
from pathlib import Path
os.environ.setdefault("TF_CPP_MIN_LOG_LEVEL","2")
os.environ.setdefault("TF_ENABLE_ONEDNN_OPTS","0")
import numpy as np
import tensorflow as tf

def main():
    ap=argparse.ArgumentParser();ap.add_argument("weights",type=Path);ap.add_argument("out",type=Path)
    ap.add_argument("--quantize",action="store_true");args=ap.parse_args();args.out.mkdir(parents=True,exist_ok=True)
    tf.config.threading.set_inter_op_parallelism_threads(2);tf.config.threading.set_intra_op_parallelism_threads(2)
    data=np.load(args.weights,allow_pickle=False)
    layers={}
    def conv(x,name):
        layer=tf.keras.layers.Conv2D(data[name+".weight"].shape[0],3 if name!="out" else 1,padding="same",name=name.replace('.','_'))
        x=layer(x);layer.set_weights([np.transpose(data[name+".weight"],(2,3,1,0)),data[name+".bias"]]);layers[name]=layer
        return x
    def block(x,name):
        return tf.nn.relu(conv(tf.nn.relu(conv(x,name+".0")),name+".2"))
    inp=tf.keras.Input(shape=(3,320,320),batch_size=1,dtype=tf.float32,name="raw_rgb")
    a=block(tf.transpose(inp[:,:1]/255.0,[0,2,3,1]),"a")
    b=block(tf.nn.max_pool2d(a,2,2,'VALID'),"b");c=block(tf.nn.max_pool2d(b,2,2,'VALID'),"c")
    d=block(tf.nn.max_pool2d(c,2,2,'VALID'),"d")
    def up(x,skip,name):
        x=tf.keras.layers.UpSampling2D(size=2,interpolation="nearest")(x)
        return block(tf.concat([x,skip],axis=-1),name)
    logits=conv(up(up(up(d,c,'u'),b,'v'),a,'w'),'out')
    keras=tf.keras.Model(inp,logits)
    samples=data['sample_inputs'];expected=data['sample_logits']
    actual=np.concatenate([np.transpose(keras(x[None],training=False).numpy(),(0,3,1,2)) for x in samples])
    maxerror=float(np.max(np.abs(actual-expected)))
    assert np.allclose(actual,expected,rtol=0.0002,atol=0.0001),maxerror
    output=tf.argmax(logits,axis=-1,output_type=tf.int64)
    deploy=tf.keras.Model(inp,output)
    manifest={'architecture':'musicsheets-tiny-unet-v1','source_weights_sha256':hashlib.sha256(args.weights.read_bytes()).hexdigest(),
              'tensorflow':tf.__version__,'keras_torch_max_abs_logit_error':maxerror,'models':{},
              'quality_approved':False,'production_install_allowed':False}
    modes=['float32','float16']+(['int8'] if args.quantize else [])
    for mode in modes:
        converter=tf.lite.TFLiteConverter.from_keras_model(deploy)
        if mode=='float16':
            converter.optimizations=[tf.lite.Optimize.DEFAULT];converter.target_spec.supported_types=[tf.float16]
        if mode=='int8':
            converter.optimizations=[tf.lite.Optimize.DEFAULT]
            converter.representative_dataset=lambda: ([x[None].astype(np.float32)] for x in samples)
            converter.target_spec.supported_ops=[tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
        payload=converter.convert();path=args.out/(mode+'.tflite');path.write_bytes(payload)
        runtime=tf.lite.Interpreter(model_path=str(path),num_threads=2);runtime.allocate_tensors()
        inputs=runtime.get_input_details()[0];outputs=runtime.get_output_details()[0]
        assert inputs['dtype']==np.float32 and inputs['shape'].tolist()==[1,3,320,320]
        assert outputs['dtype']==np.int64 and outputs['shape'].tolist()==[1,320,320]
        matches=[];elapsed=[];predictions=[]
        for i,x in enumerate(samples):
            runtime.set_tensor(inputs['index'],x[None].astype(np.float32))
            start=time.perf_counter();runtime.invoke();elapsed.append(time.perf_counter()-start)
            labels=runtime.get_tensor(outputs['index'])[0];predictions.append(labels)
            matches.append(float(np.mean(labels==expected[i].argmax(axis=0))))
        if mode=='float32':assert min(matches)>0.9999,matches
        ops=sorted(set(op['op_name'] for op in runtime._get_ops_details()))
        assert all(not op.startswith('Flex') for op in ops),ops
        manifest['models'][mode]={'bytes':len(payload),'sha256':hashlib.sha256(payload).hexdigest(),
            'input_shape':inputs['shape'].tolist(),'output_shape':outputs['shape'].tolist(),
            'agreement_with_torch':matches,'desktop_tile_seconds':elapsed,'operators':ops}
        np.save(args.out/(mode+'-labels.npy'),np.array(predictions))
    # Phone input is grayscale bytes; labels are class bytes, all little-endian independent.
    for i,x in enumerate(samples[:3]):
        (args.out/f'probe-{i}.gray').write_bytes(x[0].astype(np.uint8).tobytes())
        (args.out/f'probe-{i}.labels').write_bytes(np.load(args.out/'float32-labels.npy')[i].astype(np.uint8).tobytes())
        for mode in modes:
            (args.out/f'probe-{i}-{mode}.labels').write_bytes(np.load(args.out/(mode+'-labels.npy'))[i].astype(np.uint8).tobytes())
    (args.out/'export.json').write_text(json.dumps(manifest,indent=2),encoding='utf-8')
    print(json.dumps(manifest,indent=2),flush=True)

if __name__=='__main__':main()
