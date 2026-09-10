# Copyright 2026 Luckolite
# SPDX-License-Identifier: Apache-2.0
"""Fresh six-class segmentation network; no pretrained weights or upstream OMR code."""
import torch
from torch import nn
from torch.nn import functional as F

ARCHITECTURE = "musicsheets-tiny-unet-v1"
CLASS_NAMES = ["background", "stems_rests_barlines", "noteheads", "clefs_keys_accidentals", "staff", "other_symbols"]

def block(inputs, outputs):
    return nn.Sequential(nn.Conv2d(inputs, outputs, 3, padding=1), nn.ReLU(),
                         nn.Conv2d(outputs, outputs, 3, padding=1), nn.ReLU())

class Segmenter(nn.Module):
    def __init__(self):
        super().__init__()
        self.a=block(1,16); self.b=block(16,32); self.c=block(32,64); self.d=block(64,128)
        self.u=block(192,64); self.v=block(96,32); self.w=block(48,16)
        self.out=nn.Conv2d(16,6,1)

    def forward(self, raw_rgb):
        a=self.a(raw_rgb[:,:1]/255.0)
        b=self.b(F.max_pool2d(a,2)); c=self.c(F.max_pool2d(b,2)); d=self.d(F.max_pool2d(c,2))
        u=self.u(torch.cat([F.interpolate(d,scale_factor=2,mode="nearest"),c],1))
        v=self.v(torch.cat([F.interpolate(u,scale_factor=2,mode="nearest"),b],1))
        w=self.w(torch.cat([F.interpolate(v,scale_factor=2,mode="nearest"),a],1))
        return self.out(w)

def export_arrays(model, path, samples):
    import numpy as np
    model=model.cpu().eval()
    with torch.inference_mode(): output=model(torch.from_numpy(samples)).numpy()
    np.savez(path, **{k:v.detach().numpy() for k,v in model.state_dict().items()},
             sample_inputs=samples, sample_logits=output)
