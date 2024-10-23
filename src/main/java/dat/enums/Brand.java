package dat.enums;

/**
 * Represents the different store brands in the Salling Group
 */
public enum Brand {
    BILKA("BILKA"),
    FOETEX("FØTEX"),
    NETTO("NETTO");

    private final String displayName;

    Brand(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Safely converts a string to a Brand enum value
     * @param brandStr the brand string to convert
     * @return the corresponding Brand enum value, or null if no match
     */
    public static Brand fromString(String brandStr) {
        if (brandStr == null) return null;

        String normalized = brandStr.trim().toUpperCase()
            .replace("Ø", "OE")
            .replace("Æ", "AE")
            .replace("Å", "AA");

        return switch (normalized) {
            case "BILKA", "BILKA TOGO", "BILKA TO GO" -> BILKA;
            case "FOETEX", "FOTEX", "FØTEX", "FOETEX FOOD", "FØTEX FOOD" -> FOETEX;
            case "NETTO", "DOGNNETTO", "DØGNNETTO" -> NETTO;
            default -> null;
        };
    }
}
