package com.moulberry.flashback;

import java.nio.charset.StandardCharsets;

public class FairFlashbackConstants {
    public static final int FAIR_FLASHBACK_MAGIC = 0xF412F41;
    public static final byte[] ENCRYPTION_KEY = "FairFlashback".getBytes(StandardCharsets.UTF_8);
}
