package service.application;

public class Config {

    public static final String TWITCH_EXTENSION_KEY = System.getenv("TWITCH_EXTENSION_KEY");
    public static final Stage STAGE = Stage.valueOf(System.getenv().getOrDefault("STAGE", Stage.TEST.name()));

    public static final Long PROMO_SCORING_START = Long.valueOf(System.getenv().getOrDefault("PROMO_SCORING_START", "0"));
    public static final Long PROMO_SCORING_END = Long.valueOf(System.getenv().getOrDefault("PROMO_SCORING_END", "0"));

    public static final String RECAPTCHA_SECRET = System.getenv().getOrDefault("RECAPTCHA_SECRET", "");

    public static final String MANDRILL_API_KEY = System.getenv().getOrDefault("MANDRILL_API_KEY", "");

    public static final String CONTEST_NAME = "stockstream-challenge";
}
