"""Frozen MobileNet features and a small species head; importing never fetches weights."""

from pathlib import Path

import torch
from PIL import Image, ImageOps
from torch import nn
from torch.utils.data import Dataset
from torchvision.models import MobileNet_V3_Small_Weights, mobilenet_v3_small

WEIGHTS = MobileNet_V3_Small_Weights.IMAGENET1K_V1
PREPROCESSING = {"color": "RGB", "exifTranspose": True, "resizeShortSide": 256,
                 "crop": [224, 224], "interpolation": "bilinear", "antialias": True,
                 "scale": 1 / 255, "mean": [0.485, 0.456, 0.406], "std": [0.229, 0.224, 0.225]}


def image_transform():
    return WEIGHTS.transforms()


def build_model(class_count, pretrained=True):
    backbone = mobilenet_v3_small(weights=WEIGHTS if pretrained else None)
    feature_count = backbone.classifier[0].in_features
    backbone.classifier = nn.Identity()
    backbone.requires_grad_(False)
    return nn.Sequential(backbone, nn.Linear(feature_count, class_count)).eval()


class ImageRowsDataset(Dataset):
    def __init__(self, root, rows):
        self.root = Path(root)
        self.rows = rows
        self.transform = image_transform()

    def __len__(self):
        return len(self.rows)

    def __getitem__(self, index):
        row = self.rows[index]
        with Image.open(self.root / row["path"]) as image:
            value = self.transform(ImageOps.exif_transpose(image).convert("RGB"))
        return value, int(row["labelIndex"])
