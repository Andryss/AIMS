import pytest
import torch
from PIL import Image

from model import build_model, image_transform


def test_only_species_head_is_trainable_without_downloading_weights():
    torch.set_num_threads(2)
    model = build_model(50, pretrained=False)
    assert all(not parameter.requires_grad for parameter in model[0].parameters())
    assert all(parameter.requires_grad for parameter in model[1].parameters())
    assert model[0].training is False
    with torch.inference_mode():
        assert model(torch.zeros(1, 3, 224, 224)).shape == (1, 50)


@pytest.mark.parametrize("size", [(300, 200), (200, 300)])
def test_preprocessing_uses_rgb_and_imagenet_normalization(size):
    tensor = image_transform()(Image.new("RGB", size, "red"))
    assert tensor.shape == (3, 224, 224)
    assert tensor[:, 0, 0].tolist() == pytest.approx([(1 - 0.485) / 0.229, -0.456 / 0.224, -0.406 / 0.225])
