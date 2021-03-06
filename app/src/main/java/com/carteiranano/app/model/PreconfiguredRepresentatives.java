package com.carteiranano.app.model;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import timber.log.Timber;

/**
 * Preconfigured representatives to choose from
 */

public class PreconfiguredRepresentatives {
    private static List<String> representatives = Arrays.asList(
            "xrb_3nanobr837fp38eeuzo1gdgnz9hjtt8ebn7pgd9i9jgea1qspyq11judupjp"
    );

    public static String getRepresentative() {
        int index = new Random().nextInt(representatives.size());
        String rep = representatives.get(index);
        Timber.d("Representative: %s", rep);
        return rep;
    }
}
