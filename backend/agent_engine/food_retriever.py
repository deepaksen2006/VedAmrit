from pathlib import Path
import pandas as pd


def load_food_dataset(csv_path: str | Path) -> pd.DataFrame:
    """Load and clean the Ayurvedic food dataset."""
    csv_path = Path(csv_path)

    if not csv_path.exists():
        raise FileNotFoundError(f"Dataset not found: {csv_path}")

    df = pd.read_csv(csv_path)

    required_columns = [
        "Food_Item",
        "Vata_Recommendation",
        "Pitta_Recommendation",
        "Kapha_Recommendation",
    ]

    missing_columns = [
        column for column in required_columns
        if column not in df.columns
    ]

    if missing_columns:
        raise ValueError(
            f"Dataset is missing required columns: {', '.join(missing_columns)}"
        )

    recommendation_columns = [
        "Vata_Recommendation",
        "Pitta_Recommendation",
        "Kapha_Recommendation",
    ]

    for column in recommendation_columns:
        df[column] = df[column].fillna("Avoid").astype(str).str.strip().str.title()

    df["Food_Item"] = df["Food_Item"].fillna("Unknown Food").astype(str).str.strip()

    return df


def get_foods_for_single_dosha(
    dosha: str,
    csv_path: str | Path,
    limit: int = 30,
) -> pd.DataFrame:
    """Return foods marked Favor for a single dosha."""
    dosha = dosha.strip().title()

    if dosha not in {"Vata", "Pitta", "Kapha"}:
        raise ValueError("Dosha must be Vata, Pitta, or Kapha.")

    df = load_food_dataset(csv_path)
    recommendation_column = f"{dosha}_Recommendation"

    foods = df[
        df[recommendation_column].eq("Favor")
    ].copy()

    return foods.head(limit)


def get_foods_for_mixed_dosha(
    primary_dosha: str,
    secondary_dosha: str,
    csv_path: str | Path,
    limit: int = 30,
) -> pd.DataFrame:
    """
    Return foods favored for the primary dosha and not avoided
    for the secondary dosha.
    """
    primary_dosha = primary_dosha.strip().title()
    secondary_dosha = secondary_dosha.strip().title()

    valid_doshas = {"Vata", "Pitta", "Kapha"}

    if primary_dosha not in valid_doshas or secondary_dosha not in valid_doshas:
        raise ValueError("Doshas must be Vata, Pitta, or Kapha.")

    df = load_food_dataset(csv_path)

    primary_column = f"{primary_dosha}_Recommendation"
    secondary_column = f"{secondary_dosha}_Recommendation"

    foods = df[
        (df[primary_column].eq("Favor"))
        & (~df[secondary_column].eq("Avoid"))
    ].copy()

    # Prefer rows recommended for both doshas.
    foods["priority"] = foods[secondary_column].map({
        "Favor": 1,
        "Neutral": 2,
        "Avoid": 3,
    }).fillna(2)

    foods = foods.sort_values("priority").drop(columns=["priority"])

    return foods.head(limit)


def get_compatible_foods(
    constitution: str,
    csv_path: str | Path,
    limit: int = 30,
) -> pd.DataFrame:
    """Return foods compatible with the specified constitution."""
    constitution = constitution.strip().title()

    if constitution in {"Vata", "Pitta", "Kapha"}:
        return get_foods_for_single_dosha(constitution, csv_path, limit)
    elif "-" in constitution:
        primary_dosha, secondary_dosha = constitution.split("-")
        return get_foods_for_mixed_dosha(primary_dosha, secondary_dosha, csv_path, limit)
    else:
        raise ValueError("Constitution must be a single dosha or a mixed dosha (e.g., Vata-Pitta).")