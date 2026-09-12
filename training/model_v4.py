# Copyright 2026 Luckolite
# SPDX-License-Identifier: Apache-2.0
"""Six-class wide-context segmentation network with an auxiliary head centre output."""
import json
import numpy as np,torch
from torch import nn
from torch.nn import functional as F
ARCHITECTURE='musicsheets-wide-context-v2-experiment'
CLASS_NAMES=['background','stems_rests_barlines','noteheads','clefs_keys_accidentals','staff','other_symbols']
def block(inputs,outputs,dilations=(1,1)):
 return nn.Sequential(nn.Conv2d(inputs,outputs,3,padding=dilations[0],dilation=dilations[0]),nn.ReLU(),nn.Conv2d(outputs,outputs,3,padding=dilations[1],dilation=dilations[1]),nn.ReLU())
class Segmenter(nn.Module):
 def __init__(self,width=32,dilations=(2,4)):
  super().__init__();self.config=dict(architecture=ARCHITECTURE,widths=[width,width*2,width*4,width*8],input_size=320,dilations={'d.0':dilations[0],'d.2':dilations[1]});self.a=block(1,width);self.b=block(width,width*2);self.c=block(width*2,width*4);self.d=block(width*4,width*8,dilations);self.u=block(width*12,width*4);self.v=block(width*6,width*2);self.w=block(width*3,width);self.out=nn.Conv2d(width,6,1);self.center=nn.Conv2d(width,1,1)
 def features(self,x):
  a=self.a(x[:,:1]/255.);b=self.b(F.max_pool2d(a,2));c=self.c(F.max_pool2d(b,2));d=self.d(F.max_pool2d(c,2));u=self.u(torch.cat([F.interpolate(d,scale_factor=2,mode='nearest'),c],1));v=self.v(torch.cat([F.interpolate(u,scale_factor=2,mode='nearest'),b],1));return self.w(torch.cat([F.interpolate(v,scale_factor=2,mode='nearest'),a],1))
 def forward(self,x):return self.out(self.features(x))
 def forward_with_centres(self,x):
  w=self.features(x);return self.out(w),self.center(w)
def widen_state(source,target):
 """Exact function-preserving channel duplication when dilations are unchanged."""
 result={}
 for name,value in target.state_dict().items():
  old=source[name].detach().cpu()
  if old.ndim==4:
   output_factor=value.shape[0]//old.shape[0];input_factor=value.shape[1]//old.shape[1];assert value.shape[0]%old.shape[0]==0 and value.shape[1]%old.shape[1]==0;new=old.repeat_interleave(output_factor,0).repeat_interleave(input_factor,1)/input_factor
  else:
   assert value.numel()%old.numel()==0;new=old.repeat_interleave(value.numel()//old.numel())
  assert new.shape==value.shape,(name,new.shape,value.shape);result[name]=new
 return result
def break_clone_symmetry(model,seed=9202026,scale=1e-4):
 """Small recorded initialization noise lets duplicated channels learn independently."""
 generator=torch.Generator(device='cpu').manual_seed(seed)
 with torch.no_grad():
  for name,value in model.named_parameters():
   if name.endswith('weight') and name not in ['out.weight','center.weight']:value.add_(torch.randn(value.shape,generator=generator,dtype=value.dtype).to(value.device)*scale)
def export_arrays(model,path,samples):
 model=model.cpu().eval()
 with torch.inference_mode():semantic,centres=model.forward_with_centres(torch.from_numpy(samples))
 np.savez(path,**{k:v.detach().numpy() for k,v in model.state_dict().items()},sample_inputs=samples,sample_logits=semantic.numpy(),sample_center_logits=centres.numpy(),architecture_json=np.array(json.dumps(model.config,sort_keys=True)))
