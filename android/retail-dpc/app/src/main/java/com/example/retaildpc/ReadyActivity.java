package com.example.retaildpc;

/**
 * Not a launcher and not a HOME activity.
 * Uses its own Android task so clearing temporary HOME policy cannot tear
 * down the foreground START screen when the Device Owner is released.
 */
public final class ReadyActivity extends MainActivity { }
