from pathlib import Path
import pickle

import pandas as pd
from sklearn.metrics import accuracy_score, classification_report, confusion_matrix
from sklearn.model_selection import train_test_split
from sklearn.tree import DecisionTreeClassifier


# ---------------------------------------------------------
# Paths
# ---------------------------------------------------------

PROJECT_ROOT = Path(__file__).resolve().parents[1]

DATA_PATH = PROJECT_ROOT / "data" / "disease" / "disease_symptoms.csv"
MODEL_DIR = PROJECT_ROOT / "models"
MODEL_PATH = MODEL_DIR / "disease_decision_tree.pkl"
FEATURE_PATH = MODEL_DIR / "disease_features.pkl"


# ---------------------------------------------------------
# Load dataset
# ---------------------------------------------------------

print("Loading dataset...")

df = pd.read_csv(DATA_PATH)

# Remove duplicate rows
df = df.drop_duplicates().reset_index(drop=True)

print(f"Dataset shape: {df.shape}")


# ---------------------------------------------------------
# Separate features and target
# ---------------------------------------------------------

FEATURE_COLUMNS = list(df.columns[:31])
TARGET_COLUMN = df.columns[31]

X = df[FEATURE_COLUMNS]
y = df[TARGET_COLUMN]


print(f"Number of features: {len(FEATURE_COLUMNS)}")
print(f"Target column: {TARGET_COLUMN}")
print(f"Classes: {sorted(y.unique())}")


# ---------------------------------------------------------
# Train/test split
# ---------------------------------------------------------

X_train, X_test, y_train, y_test = train_test_split(
    X,
    y,
    test_size=0.20,
    random_state=42,
    stratify=y,
)


# ---------------------------------------------------------
# Train Decision Tree
# ---------------------------------------------------------

print("\nTraining Decision Tree...")

model = DecisionTreeClassifier(
    random_state=42
)

model.fit(X_train, y_train)


# ---------------------------------------------------------
# Evaluate
# ---------------------------------------------------------

y_pred = model.predict(X_test)

accuracy = accuracy_score(y_test, y_pred)

print("\n==============================")
print("DISEASE MODEL RESULTS")
print("==============================")
print(f"Accuracy: {accuracy:.4f}")
print(f"Accuracy (%): {accuracy * 100:.2f}%")

print("\nClassification Report:")
print(classification_report(y_test, y_pred))

print("Confusion Matrix:")
print(confusion_matrix(y_test, y_pred))


# ---------------------------------------------------------
# Save model
# ---------------------------------------------------------

MODEL_DIR.mkdir(parents=True, exist_ok=True)

with open(MODEL_PATH, "wb") as file:
    pickle.dump(model, file)

with open(FEATURE_PATH, "wb") as file:
    pickle.dump(FEATURE_COLUMNS, file)


print("\n==============================")
print("MODEL SAVED SUCCESSFULLY")
print("==============================")
print(f"Model: {MODEL_PATH}")
print(f"Features: {FEATURE_PATH}")
