package com.ejpapas.cobysmartremote.model

/**
 * Every remote button. [androidKeyCode] is the standard android.view.KeyEvent code that is
 * injected on the TV via `input keyevent` when using the Wi-Fi (ADB) method.
 *
 * Nothing here is "known to work" on the Star Coby TV. A key only becomes usable on the
 * Remote screen after you test it in Device Setup and the result is verified.
 */
enum class RemoteKey(val label: String, val androidKeyCode: Int) {
    VOLUME_UP("Volume up", 24),
    VOLUME_DOWN("Volume down", 25),
    MUTE("Mute", 164),
    HOME("Home", 3),
    BACK("Back", 4),
    DPAD_UP("Up", 19),
    DPAD_DOWN("Down", 20),
    DPAD_LEFT("Left", 21),
    DPAD_RIGHT("Right", 22),
    OK("OK / Enter", 23),
    MENU("Menu", 82),
    SETTINGS("Settings", 176),
    SOURCE("Source / Input", 178),
    POWER("Power", 26),
    CHANNEL_UP("Channel up", 166),
    CHANNEL_DOWN("Channel down", 167),
    PLAY_PAUSE("Play / Pause", 85),
    REWIND("Rewind", 89),
    FAST_FORWARD("Fast-forward", 90),
    NUM_0("0", 7),
    NUM_1("1", 8),
    NUM_2("2", 9),
    NUM_3("3", 10),
    NUM_4("4", 11),
    NUM_5("5", 12),
    NUM_6("6", 13),
    NUM_7("7", 14),
    NUM_8("8", 15),
    NUM_9("9", 16),
}
