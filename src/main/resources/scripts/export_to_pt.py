import os
import torch
import torch.nn as nn
import numpy as np
import sys

MODEL_DIR = sys.argv[1] if len(sys.argv) > 1 else "models/coin-sniper"
WEIGHTS_DIR = os.path.join(MODEL_DIR, "weights")
PT_FILE = os.path.join(MODEL_DIR, "coin-sniper-model.pt")

# Ensure model directory exists
os.makedirs(MODEL_DIR, exist_ok=True)

class CoinSniperModel(nn.Module):
    def __init__(self):
        super().__init__()
        self.fc1 = nn.Linear(1, 8)
        self.relu = nn.ReLU()
        self.fc2 = nn.Linear(8, 2)

    def forward(self, x):
        return self.fc2(self.relu(self.fc1(x)))

model = CoinSniperModel()

if os.path.exists(WEIGHTS_DIR):
    def load_csv(name):
        return np.loadtxt(os.path.join(WEIGHTS_DIR, name), delimiter=",")

    with torch.no_grad():
        model.fc1.weight.copy_(torch.tensor(load_csv("w1.csv")).reshape(8, 1))
        model.fc1.bias.copy_(torch.tensor(load_csv("b1.csv")))
        model.fc2.weight.copy_(torch.tensor(load_csv("w2.csv")).reshape(2, 8))
        model.fc2.bias.copy_(torch.tensor(load_csv("b2.csv")))
    print("Weights loaded.")
else:
    print("Weights not found. Using random weights.")

scripted = torch.jit.trace(model, torch.randn(1, 1))
scripted.save(PT_FILE)
print(f"TorchScript model saved at {PT_FILE}")
