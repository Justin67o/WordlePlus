// The window dimensions are 720 x 950
// The rules are that you have 6 chances to guess a random word. 
// The game will be singleplayer and the player gets to choose between different gamemodes: 
//   - standard (6 chances, 5 letter word), 
//   - unlimited (infinite chances), 
//   - custom (6 chances, 3 letter to 8 letter words),
//   - timed (2 minutes, 3 minutes, 5 minutes), highscores are tracked

import java.util.*;
import java.io.*;

import java.awt.event.*;
import java.awt.*;
import javax.swing.*;

import javax.imageio.*;
import java.awt.image.*;

import java.awt.geom.AffineTransform;

import java.nio.file.Paths;
import java.nio.file.Files;

import java.io.File;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

@SuppressWarnings("serial")
public class WordlePlus extends JPanel implements Runnable, KeyListener, MouseListener, MouseMotionListener {
    // Game variables
    final int FPS = 60;
    final int SCREEN_WIDTH = 720, SCREEN_HEIGHT = 950;
    final int NONE = -1;
    Thread thread;
    boolean gameOver;
    boolean gameWon;

    // Game state enumeration
    final int STATE_MAIN_MENU = 0;
    final int STATE_GAMEMODES_MENU = 1;
    final int STATE_SETTINGS_MENU = 2;
    final int STATE_GAME = 3;
    final int STATE_RULES_MENU = 4;
    int state;

    // Colors
    Color backgroundColor;
    Color textColor;
    Color buttonColor, buttonHoverColor;
    Color buttonPlayAgainColor, buttonPlayAgainHoverColor;
    Color wordsGridBorderColor, wordsGridBorderDeepColor;
    Color wordsGridExactColor, wordsGridCloseColor, wordsGridWrongColor; 
    Color popupColor;

    // Buttons
    final int BUTTON_MAIN_MENU_START = 0, BUTTON_MAIN_MENU_GAMEMODES = 1, BUTTON_MAIN_MENU_SETTINGS = 2, BUTTON_BACK = 3;
    final int BUTTON_GAMEMODES_MENU_STANDARD = 4, BUTTON_GAMEMODES_MENU_3 = 5, BUTTON_GAMEMODES_MENU_4 = 6, BUTTON_GAMEMODES_MENU_5 = 7, BUTTON_GAMEMODES_MENU_6 = 8, BUTTON_GAMEMODES_MENU_7 = 9, BUTTON_GAMEMODES_MENU_8 = 10;
    final int BUTTON_MAIN_MENU_RULES = 13;
    final int BUTTON_GAMEMODES_MENU_UNLIMITED = 11;
    final int BUTTON_PLAY_AGAIN = 12;
    final int BUTTON_GAMEMODES_MENU_TIMED_30_SECONDS = 300, BUTTON_GAMEMODES_MENU_TIMED_45_SECONDS = 301, BUTTON_GAMEMODES_MENU_TIMED_1_MINUTE = 302;
    int button;

    // Words
    String[] wordsList;
    String wordGenerated;
    int wordLength;
    int tries;
    int triesLeft;
    ArrayList<ArrayList<Character>> wordsEntered;
    Color[][] wordsGrid;
    Color[][] wordsGridBorder;

    final int LETTER_WRONG = 0, LETTER_CLOSE = 1, LETTER_EXACT = 2;

    // Gamemode enumeration
    final int GAMEMODE_STANDARD = 0;
    final int GAMEMODE_UNLIMITED = 1;
    final int GAMEMODE_3 = 2, GAMEMODE_4 = 3, GAMEMODE_5 = 4, GAMEMODE_6 = 5, GAMEMODE_7 = 6, GAMEMODE_8 = 7;
    final int GAMEMODE_TIMED_30_SECONDS = 8, GAMEMODE_TIMED_45_SECONDS = 9, GAMEMODE_TIMED_1_MINUTE = 10;
    int gamemode;

    // Letter animations
    boolean flippingLetters;
    int flippingLetter;
    float flippingLetterHeight;
    float t_flippingLetterHeight = 0.0f;
    float[] letterScalings;

    // Statistics
    int statisticsPlayed;
    float statisticsWin;
    int statisticsCurrentStreak;
    int statisticsMaxStreak;

    // Popup
    String popup;
    float popupOpacity;
    float t_popupOpacity;

    float lettersPositionOffset;
    float t_lettersPositionOffset;

    // Settings
    final int BUTTON_SETTINGS_DARK_THEME = 100, BUTTON_SETTINGS_MUSIC = 101, BUTTON_SETTINGS_REDUCE_ANIMATIONS = 102, BUTTON_SETTINGS_SOUND_EFFECTS = 103, BUTTON_SETTINGS_PRINT_GENERATED_WORD = 104;
    boolean settingsDarkTheme;
    boolean settingsMusic;
    boolean settingsReduceAnimations;
    boolean settingsSoundEffects;
    boolean settingsPrintGeneratedWord;

    // Keyboard variables
    int[] lettersEntered;
    int[] lettersEnteredTmp;

    // Sound variables
    Clip music;
    Clip winSound;
    Clip loseSound;

    // Time elapsed
    long timePrevious;
    long timeCurrent;
    int timeElapsed;
    int timeLimit;

    public WordlePlus() {
        // Set up JPanel
        setPreferredSize(new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT));
        setBackground(backgroundColor);
        setVisible(true);

        // Start a new thread
        thread = new Thread(this);
        thread.start();

        addMouseListener(this);
        addMouseMotionListener(this);
    }

    @Override
    public void run() {
        // Initialize the game
        initialize();

        // Main game loop
        while (true) {
            update();
            this.repaint();
            try {
                Thread.sleep(1000 / FPS); // (1000 * 1 / FPS) = 16.66666667 ms
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Sets up the game before it starts rsunning
    public void initialize() {
        // Set the state
        state = STATE_MAIN_MENU;

        // Set the gamemode
        gamemode = GAMEMODE_STANDARD;

        // Set the menu button state
        button = NONE;

        // Set up game
        gameOver = false;
        gameWon = false;

        // Set up letter animation variables
        flippingLetters = false;
        flippingLetter = 0;
        flippingLetterHeight = 1.0f;
        t_flippingLetterHeight = 0.0f;

        popupOpacity = 0.0f;
        t_popupOpacity = 0.0f;

        // Create the statistics files
        try {
            Files.createDirectories(Paths.get("./statistics/"));

            File statisticsPlayedFile = new File("statistics/played.txt");
            statisticsPlayedFile.createNewFile();

            File statisticsWinFile = new File("statistics/win.txt");
            statisticsWinFile.createNewFile();

            File statisticsCurrentStreakFile = new File("statistics/currentStreak.txt");
            statisticsCurrentStreakFile.createNewFile();

            File statisticsMaxStreakFile = new File("statistics/maxStreak.txt");
            statisticsMaxStreakFile.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Create the settings files 
        try {
            Files.createDirectories(Paths.get("./settings/"));

            File file = new File("settings/darkTheme.txt");
            file.createNewFile();

            file = new File("settings/music.txt");
            file.createNewFile();

            file = new File("settings/reduceAnimations.txt");
            file.createNewFile();

            file = new File("settings/soundEffects.txt");
            file.createNewFile();

            file = new File("settings/printGeneratedWord.txt");
            file.createNewFile();

            loadSettings();
            updateSettings();
        } catch (Exception e) {
            e.printStackTrace();
        }

        updateThemeColors();

        // Load the sounds
        try {
            AudioInputStream sound = AudioSystem.getAudioInputStream(new File("sounds/music.wav"));
            music = AudioSystem.getClip();
            music.open(sound);
            if (settingsMusic) {
                music.start();
                music.loop(Clip.LOOP_CONTINUOUSLY);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Simulates and updates one frame of the game
    public void update() {
        // Update time
        if (state == STATE_GAME && (gamemode == GAMEMODE_TIMED_30_SECONDS || gamemode == GAMEMODE_TIMED_45_SECONDS || gamemode == GAMEMODE_TIMED_1_MINUTE)) {
            timeCurrent = System.currentTimeMillis();
            timeElapsed += timeCurrent - timePrevious;
            timePrevious = timeCurrent;

            // If the player runs out of time
            if (!gameOver && timeLimit - (int) timeElapsed / 1000 <= 0) {
                gameOver = true;
                gameWon = false;
                updateStatistics();
                if (settingsSoundEffects) {
                    try {
                        AudioInputStream sound = AudioSystem.getAudioInputStream(new File("sounds/loseSound.wav"));
                        loseSound = AudioSystem.getClip();
                        loseSound.open(sound);
                        loseSound.start();
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }
                }
            }
        }

        // Flipping letters animation
        if (state == STATE_GAME) {
            // Handle flipping letters animation
            if (flippingLetters) {
                if (flippingLetter < wordLength) {
                    if (flippingLetterHeight < -1.0f) {
                        flippingLetter++;
                        flippingLetterHeight = 1.0f;
                        t_flippingLetterHeight = 0.0f;
                        return;
                    }
                    t_flippingLetterHeight += 0.005f;
                    float v1 = 0.1f + (float) Math.tanh(t_flippingLetterHeight);
                    float v2 = 1.0f - (t_flippingLetterHeight - 1.0f) * (t_flippingLetterHeight - 1.0f);
                    flippingLetterHeight -= lerp(v1, v2, t_flippingLetterHeight);
                } else {
                    flippingLetters = false;
                    flippingLetter = 0;
                    flippingLetterHeight = 1.0f;
                    t_flippingLetterHeight = 0.0f;
                    for (int i = 0; i < 26; i++) {
                        lettersEntered[i] = lettersEnteredTmp[i];
                    }
                }
            }

            // Handle popup animation
            if (popupOpacity > 0.0f && popupOpacity <= 1.0f) {
                popupOpacity = 1.0f / (20 * (t_popupOpacity - 3)) + 1.016f;
                t_popupOpacity += 0.05f;
            } else {
                popupOpacity = 0.0f;
                t_popupOpacity = 0.0f;
            }

            // Handle letter bubbling animation
            for (int i = 0; i < wordLength; i++) {
                if (letterScalings[i] > 1.0f && letterScalings[i] < 1.13f) {
                    letterScalings[i] += 0.05f;
                } else {
                    letterScalings[i] = 1.0f;
                }
            }

            // Handle letters shaking effect
            if (lettersPositionOffset != 0.0f) {
                t_lettersPositionOffset += 0.1f;
                lettersPositionOffset = (float) (15.0f / Math.pow(2, t_lettersPositionOffset) * Math.cos(10 * t_lettersPositionOffset));
                if (t_lettersPositionOffset > 3.0f) {
                    t_lettersPositionOffset = 0.0f;
                    lettersPositionOffset = 0.0f;
                }
            }
        }
    }

    /*
     * Draw graphics
     */
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Handle drawing for different states
        if (state == STATE_MAIN_MENU) {
            // Draw title
            drawCenteredString(
                g, 
                "Wordle+", 
                new Font("Arial", Font.BOLD, 70),
                textColor,
                SCREEN_WIDTH / 2, 300
            );

            // Draw menu buttons
            drawButton(
                g,
                "Start",
                new Font("Arial", Font.PLAIN, 30),
                textColor,
                button == BUTTON_MAIN_MENU_START ? buttonHoverColor : buttonColor,
                SCREEN_WIDTH / 2, 400,
                200, 50
            );
            drawButton(
                g,
                "Gamemodes",
                new Font("Arial", Font.PLAIN, 30),
                textColor,
                button == BUTTON_MAIN_MENU_GAMEMODES ? buttonHoverColor : buttonColor,
                SCREEN_WIDTH / 2, 470,
                200, 50
            );
            drawButton(
                g,
                "Settings",
                new Font("Arial", Font.PLAIN, 30),
                textColor,
                button == BUTTON_MAIN_MENU_SETTINGS ? buttonHoverColor : buttonColor,
                SCREEN_WIDTH / 2, 540,
                200, 50
            );
            drawButton(
                g,
                "Rules",
                new Font("Arial", Font.PLAIN, 30),
                textColor,
                button == BUTTON_MAIN_MENU_RULES ? buttonHoverColor : buttonColor,
                SCREEN_WIDTH / 2, 610,
                200, 50
            );

            // Draw gamemode
            String currentGamemode = "";
            if (gamemode == GAMEMODE_STANDARD) {
                currentGamemode = "Standard";
            } else if (gamemode == GAMEMODE_UNLIMITED) {
                currentGamemode = "Unlimited";
            } else if (gamemode == GAMEMODE_3) {
                currentGamemode = "3 Letters";
            } else if (gamemode == GAMEMODE_4) {
                currentGamemode = "4 Letters";
            } else if (gamemode == GAMEMODE_5) {
                currentGamemode = "5 Letters";
            } else if (gamemode == GAMEMODE_6) {
                currentGamemode = "6 Letters";
            } else if (gamemode == GAMEMODE_7) {
                currentGamemode = "7 Letters";
            } else if (gamemode == GAMEMODE_8) {
                currentGamemode = "8 Letters";
            } else if (gamemode == GAMEMODE_TIMED_30_SECONDS) {
                currentGamemode = "30 Seconds";
            } else if (gamemode == GAMEMODE_TIMED_45_SECONDS) {
                currentGamemode = "45 Seconds";
            } else if (gamemode == GAMEMODE_TIMED_1_MINUTE) {
                currentGamemode = "1 Minute";
            }
            drawCenteredString(
                g,
                "Gamemode: " + currentGamemode,
                new Font("Arial", Font.PLAIN, 30),
                textColor,
                SCREEN_WIDTH / 2, 670
            );
        } else if (state == STATE_GAME) {
            // Draw title
            drawCenteredString(
                g, 
                "Wordle+", 
                new Font("Arial", Font.BOLD, 50),
                textColor,
                SCREEN_WIDTH / 2, 50
            );

            // Draw if the game is over or not
            if (!gameOver || flippingLetters) {
                // Draw the letters grid
                drawWordsGrid(g);

                // Draw back button
                drawButton(
                    g,
                    "Back",
                    new Font("Arial", Font.PLAIN, 20),
                    textColor,
                    button == BUTTON_BACK ? buttonHoverColor : buttonColor,
                    50, 30,
                    70, 30
                );

                // Handle timed gamemode
                if (gamemode == GAMEMODE_TIMED_30_SECONDS || gamemode == GAMEMODE_TIMED_45_SECONDS || gamemode == GAMEMODE_TIMED_1_MINUTE) {
                    int timeLeft = timeLimit - (int) timeElapsed / 1000;
                    drawCenteredString(
                        g,
                        "Time: " + (timeLeft / 60) + ":" + (timeLeft % 60 >= 10 ? timeLeft % 60 : "0" + timeLeft % 60),
                        new Font("Arial", Font.BOLD, 30),
                        timeLeft < 30 && timeLeft % 2 != 0 ? new Color(234, 105, 100) : textColor,
                        SCREEN_WIDTH / 2, 110
                    );
                }

                // Draw the noInWordsList
                if (popupOpacity > 0.0f) {
                    drawButton(
                        g,
                        popup,
                        new Font("Arial", Font.BOLD, 20),
                        backgroundColor,
                        new Color(
                            (int) (lerp(backgroundColor.getRed(), popupColor.getRed(), popupOpacity)), 
                            (int) (lerp(backgroundColor.getGreen(), popupColor.getGreen(), popupOpacity)), 
                            (int) (lerp(backgroundColor.getBlue(), popupColor.getBlue(), popupOpacity))
                        ),
                        SCREEN_WIDTH / 2, 110,
                        190, 50
                    );
                }

                // Draw the keyboard
                drawKeyboard(g);
            } else {
                // Draw title
                drawCenteredString(
                    g, 
                    gameWon ? "You Win!" : "You Lose!", 
                    new Font("Arial", Font.BOLD, 30),
                    gameWon ? wordsGridExactColor : new Color(234, 105, 100),
                    SCREEN_WIDTH / 2, 150
                );
                drawCenteredString(
                    g,
                    "The word was \"" + wordGenerated + "\"",
                    new Font("Arial", Font.BOLD, 20),
                    textColor,
                    SCREEN_WIDTH / 2, 180
                );

                // Draw the statistics
                drawCenteredString(
                    g, 
                    "Statistics", 
                    new Font("Arial", Font.BOLD, 30),
                    textColor,
                    SCREEN_WIDTH / 2, 250
                );

                // Draw games played
                drawCenteredString(
                    g,
                    "Played",
                    new Font("Arial", Font.PLAIN, 20),
                    textColor,
                    SCREEN_WIDTH / 2, 290
                );
                drawCenteredString(
                    g,
                    "" + statisticsPlayed,
                    new Font("Arial", Font.PLAIN, 20),
                    textColor,
                    SCREEN_WIDTH / 2, 320
                );

                // Draw win %
                drawCenteredString(
                    g,
                    "Win %",
                    new Font("Arial", Font.PLAIN, 20),
                    textColor,
                    SCREEN_WIDTH / 2, 360
                );
                drawCenteredString(
                    g,
                    (int) (100 * statisticsWin) + "%",
                    new Font("Arial", Font.PLAIN, 20),
                    textColor,
                    SCREEN_WIDTH / 2, 390
                );

                // Draw current streak
                drawCenteredString(
                    g,
                    "Current Streak",
                    new Font("Arial", Font.PLAIN, 20),
                    textColor,
                    SCREEN_WIDTH / 2, 430
                );
                drawCenteredString(
                    g,
                    "" + statisticsCurrentStreak,
                    new Font("Arial", Font.PLAIN, 20),
                    textColor,
                    SCREEN_WIDTH / 2, 460
                );

                // Draw max streak
                drawCenteredString(
                    g,
                    "Max Streak",
                    new Font("Arial", Font.PLAIN, 20),
                    textColor,
                    SCREEN_WIDTH / 2, 500
                );
                drawCenteredString(
                    g,
                    "" + statisticsMaxStreak,
                    new Font("Arial", Font.PLAIN, 20),
                    textColor,
                    SCREEN_WIDTH / 2, 530
                );
                
                // Play again button
                drawButton(
                    g,
                    "Play Again",
                    new Font("Arial", Font.PLAIN, 20),
                    textColor,
                    button == BUTTON_PLAY_AGAIN ? buttonPlayAgainHoverColor : buttonPlayAgainColor,
                    SCREEN_WIDTH / 2 - 55, 640,
                    120, 30
                );

                // Back button
                drawButton(
                    g,
                    "Back",
                    new Font("Arial", Font.PLAIN, 20),
                    textColor,
                    button == BUTTON_BACK ? buttonHoverColor : buttonColor,
                    SCREEN_WIDTH / 2 + 55, 640,
                    70, 30
                );
            }
        } else if (state == STATE_GAMEMODES_MENU) {
            // Draw title
            drawCenteredString(
                g, 
                "Wordle+", 
                new Font("Arial", Font.BOLD, 50),
                textColor,
                SCREEN_WIDTH / 2, 50
            );
            drawCenteredString(
                g, 
                "Gamemodes", 
                new Font("Arial", Font.BOLD, 40),
                textColor,
                SCREEN_WIDTH / 2, 150
            );

            // Draw back button
            drawButton(
                g,
                "Back",
                new Font("Arial", Font.PLAIN, 20),
                textColor,
                button == BUTTON_BACK ? buttonHoverColor : buttonColor,
                50, 30,
                70, 30
            );

            // Draw gamemode buttons
            drawButton(
                g,
                "Standard",
                new Font("Arial", Font.PLAIN, 30),
                textColor,
                button == BUTTON_GAMEMODES_MENU_STANDARD ? buttonHoverColor : buttonColor,
                SCREEN_WIDTH / 2, 220,
                200, 50
            );
            drawButton(
                g,
                "Unlimited",
                new Font("Arial", Font.PLAIN, 30),
                textColor,
                button == BUTTON_GAMEMODES_MENU_UNLIMITED ? buttonHoverColor : buttonColor,
                SCREEN_WIDTH / 2, 280,
                200, 50
            );
            drawButton(
                g,
                "3 Letters",
                new Font("Arial", Font.PLAIN, 30),
                textColor,
                button == BUTTON_GAMEMODES_MENU_3 ? buttonHoverColor : buttonColor,
                SCREEN_WIDTH / 2 - 200 - 10, 340,
                200, 50
            );
            drawButton(
                g,
                "4 Letters",
                new Font("Arial", Font.PLAIN, 30),
                textColor,
                button == BUTTON_GAMEMODES_MENU_4 ? buttonHoverColor : buttonColor,
                SCREEN_WIDTH / 2, 340,
                200, 50
            );
            drawButton(
                g,
                "5 Letters",
                new Font("Arial", Font.PLAIN, 30),
                textColor,
                button == BUTTON_GAMEMODES_MENU_5 ? buttonHoverColor : buttonColor,
                SCREEN_WIDTH / 2 + 200 + 10, 340,
                200, 50
            );
            drawButton(
                g,
                "6 Letters",
                new Font("Arial", Font.PLAIN, 30),
                textColor,
                button == BUTTON_GAMEMODES_MENU_6 ? buttonHoverColor : buttonColor,
                SCREEN_WIDTH / 2 - 200 - 10, 340 + 50 + 10,
                200, 50
            );
            drawButton(
                g,
                "7 Letters",
                new Font("Arial", Font.PLAIN, 30),
                textColor,
                button == BUTTON_GAMEMODES_MENU_7 ? buttonHoverColor : buttonColor,
                SCREEN_WIDTH / 2, 340 + 50 + 10,
                200, 50
            );
            drawButton(
                g,
                "8 Letters",
                new Font("Arial", Font.PLAIN, 30),
                textColor,
                button == BUTTON_GAMEMODES_MENU_8 ? buttonHoverColor : buttonColor,
                SCREEN_WIDTH / 2 + 200 + 10, 340 + 50 + 10,
                200, 50
            );

            // Draw timed gamemode
            drawButton(
                g,
                "30 Seconds",
                new Font("Arial", Font.PLAIN, 30),
                textColor,
                button == BUTTON_GAMEMODES_MENU_TIMED_30_SECONDS ? buttonHoverColor : buttonColor,
                SCREEN_WIDTH / 2 - 200 - 10, 400 + 50 + 10,
                200, 50
            );
            drawButton(
                g,
                "45 Seconds",
                new Font("Arial", Font.PLAIN, 30),
                textColor,
                button == BUTTON_GAMEMODES_MENU_TIMED_45_SECONDS ? buttonHoverColor : buttonColor,
                SCREEN_WIDTH / 2, 400 + 50 + 10,
                200, 50
            );
            drawButton(
                g,
                "1 Minute",
                new Font("Arial", Font.PLAIN, 30),
                textColor,
                button == BUTTON_GAMEMODES_MENU_TIMED_1_MINUTE ? buttonHoverColor : buttonColor,
                SCREEN_WIDTH / 2 + 200 + 10, 400 + 50 + 10,
                200, 50
            );
        } else if (state == STATE_SETTINGS_MENU) {
            // Draw title
            drawCenteredString(
                g, 
                "Wordle+", 
                new Font("Arial", Font.BOLD, 50),
                textColor,
                SCREEN_WIDTH / 2, 50
            );
            drawCenteredString(
                g, 
                "Settings", 
                new Font("Arial", Font.BOLD, 40),
                textColor,
                SCREEN_WIDTH / 2, 150
            );

            // Draw back button
            drawButton(
                g,
                "Back",
                new Font("Arial", Font.PLAIN, 20),
                textColor,
                button == BUTTON_BACK ? buttonHoverColor : buttonColor,
                50, 30,
                70, 30
            );

            // Settings
            drawLeftAlignedString(
                g,
                "Dark Theme",
                new Font("Arial", Font.PLAIN, 30),
                textColor,
                SCREEN_WIDTH / 4, 300
            );
            drawSwitch(
                g,
                (int) (SCREEN_WIDTH * 0.75), 300,
                35, 20,
                settingsDarkTheme
            );
            drawLeftAlignedString(
                g,
                "Music",
                new Font("Arial", Font.PLAIN, 30),
                textColor,
                SCREEN_WIDTH / 4, 360
            );
            drawSwitch(
                g,
                (int) (SCREEN_WIDTH * 0.75), 360,
                35, 20,
                settingsMusic
            );
            drawLeftAlignedString(
                g,
                "Reduce Animations",
                new Font("Arial", Font.PLAIN, 30),
                textColor,
                SCREEN_WIDTH / 4, 420
            );
            drawSwitch(
                g,
                (int) (SCREEN_WIDTH * 0.75), 420,
                35, 20,
                settingsReduceAnimations
            );
            drawLeftAlignedString(
                g,
                "Sound Effects",
                new Font("Arial", Font.PLAIN, 30),
                textColor,
                SCREEN_WIDTH / 4, 480
            );
            drawSwitch(
                g,
                (int) (SCREEN_WIDTH * 0.75), 480,
                35, 20,
                settingsSoundEffects
            );
            drawLeftAlignedString(
                g,
                "Print Generated Word",
                new Font("Arial", Font.PLAIN, 30),
                textColor,
                SCREEN_WIDTH / 4, 540
            );
            drawSwitch(
                g,
                (int) (SCREEN_WIDTH * 0.75), 540,
                35, 20,
                settingsPrintGeneratedWord
            );
        } else if (state == STATE_RULES_MENU) {
            // Draw title
            drawCenteredString(
                g, 
                "Wordle+", 
                new Font("Arial", Font.BOLD, 50),
                textColor,
                SCREEN_WIDTH / 2, 50
            );
            drawCenteredString(
                g, 
                "Rules", 
                new Font("Arial", Font.BOLD, 40),
                textColor,
                SCREEN_WIDTH / 2, 150
            );

            // Draw back button
            drawButton(
                g,
                "Back",
                new Font("Arial", Font.PLAIN, 20),
                textColor,
                button == BUTTON_BACK ? buttonHoverColor : buttonColor,
                50, 30,
                70, 30
            );

            // Draw rules
            drawLeftAlignedString(
                g,
                "Guess the Wordle in a set number of tries.",
                new Font("Arial", Font.PLAIN, 20),
                textColor,
                30, 200
            );
            drawLeftAlignedString(
                g,
                "Each guess must be a valid word and contain enough letters.",
                new Font("Arial", Font.PLAIN, 20),
                textColor,
                30, 230
            );
            drawLeftAlignedString(
                g,
                "The color of the tiles will change depending on how close your guess was.",
                new Font("Arial", Font.PLAIN, 20),
                textColor,
                30, 260
            );
            drawLeftAlignedString(
                g,
                "Examples",
                new Font("Arial", Font.BOLD, 20),
                textColor,
                30, 300
            );

            // Draw the wordle examples
            {
                int lx = 30, ly = 320;
                int cell_w = 40, cell_h = 40;
                int cell_gap_lr = 4;
                int cell_border_w = 2;
                Color[] gridColors = {
                    wordsGridExactColor,
                    backgroundColor,
                    backgroundColor,
                    backgroundColor,
                    backgroundColor
                };
                Color[] gridBorderColors = {
                    wordsGridExactColor,
                    wordsGridBorderDeepColor,
                    wordsGridBorderDeepColor,
                    wordsGridBorderDeepColor,
                    wordsGridBorderDeepColor
                };
                String word = "WEARY";
                for (int i = 0; i < 5; i++) {
                    g.setColor(gridColors[i]);
                    g.fillRect(
                        lx, ly,
                        cell_w, cell_h
                    );
                    g.setColor(gridBorderColors[i]);
                    for (int j = 0; j < cell_border_w; j++) {
                        g.drawRect(
                            lx + j, ly + j,
                            cell_w - 2 * j, cell_h - 2 * j
                        );
                    }

                    String letter = "" + word.charAt(i);
                    Font font = new Font("Arial", Font.BOLD, 22);
                    g.setFont(font);
                    drawCenteredString(
                        g,
                        letter,
                        font,
                        textColor,
                        lx + cell_w / 2, ly + cell_h / 2
                    );

                    lx += cell_w + cell_gap_lr;
                }

                drawLeftAlignedString(
                    g,
                    "W is in the word and in the correct spot.",
                    new Font("Arial", Font.PLAIN, 20),
                    textColor,
                    30, 380
                );
            }

            {
                int lx = 30, ly = 400;
                int cell_w = 40, cell_h = 40;
                int cell_gap_lr = 4;
                int cell_border_w = 2;
                Color[] gridColors = {
                    backgroundColor,
                    wordsGridCloseColor,
                    backgroundColor,
                    backgroundColor,
                    backgroundColor
                };
                Color[] gridBorderColors = {
                    wordsGridBorderDeepColor,
                    wordsGridCloseColor,
                    wordsGridBorderDeepColor,
                    wordsGridBorderDeepColor,
                    wordsGridBorderDeepColor
                };
                String word = "PILLS";
                for (int i = 0; i < 5; i++) {
                    g.setColor(gridColors[i]);
                    g.fillRect(
                        lx, ly,
                        cell_w, cell_h
                    );
                    g.setColor(gridBorderColors[i]);
                    for (int j = 0; j < cell_border_w; j++) {
                        g.drawRect(
                            lx + j, ly + j,
                            cell_w - 2 * j, cell_h - 2 * j
                        );
                    }

                    String letter = "" + word.charAt(i);
                    Font font = new Font("Arial", Font.BOLD, 22);
                    g.setFont(font);
                    drawCenteredString(
                        g,
                        letter,
                        font,
                        textColor,
                        lx + cell_w / 2, ly + cell_h / 2
                    );

                    lx += cell_w + cell_gap_lr;
                }

                drawLeftAlignedString(
                    g,
                    "I is in the word but in the wrong spot.",
                    new Font("Arial", Font.PLAIN, 20),
                    textColor,
                    30, 460
                );
            }

            {
                int lx = 30, ly = 480;
                int cell_w = 40, cell_h = 40;
                int cell_gap_lr = 4;
                int cell_border_w = 2;
                Color[] gridColors = {
                    backgroundColor,
                    backgroundColor,
                    backgroundColor,
                    wordsGridWrongColor,
                    backgroundColor
                };
                Color[] gridBorderColors = {
                    wordsGridBorderDeepColor,
                    wordsGridBorderDeepColor,
                    wordsGridBorderDeepColor,
                    wordsGridWrongColor,
                    wordsGridBorderDeepColor
                };
                String word = "VAGUE";
                for (int i = 0; i < 5; i++) {
                    g.setColor(gridColors[i]);
                    g.fillRect(
                        lx, ly,
                        cell_w, cell_h
                    );
                    g.setColor(gridBorderColors[i]);
                    for (int j = 0; j < cell_border_w; j++) {
                        g.drawRect(
                            lx + j, ly + j,
                            cell_w - 2 * j, cell_h - 2 * j
                        );
                    }

                    String letter = "" + word.charAt(i);
                    Font font = new Font("Arial", Font.BOLD, 22);
                    g.setFont(font);
                    drawCenteredString(
                        g,
                        letter,
                        font,
                        textColor,
                        lx + cell_w / 2, ly + cell_h / 2
                    );

                    lx += cell_w + cell_gap_lr;
                }

                drawLeftAlignedString(
                    g,
                    "U is not in the word in any spot.",
                    new Font("Arial", Font.PLAIN, 20),
                    textColor,
                    30, 540
                );
            }

            drawLeftAlignedString(
                g,
                "Gamemodes can be chosen from the gamemodes menu.",
                new Font("Arial", Font.PLAIN, 20),
                textColor,
                30, 600
            );
            drawLeftAlignedString(
                g,
                "Settings can be tweaked in the settings menu.",
                new Font("Arial", Font.PLAIN, 20),
                textColor,
                30, 630
            );
        }
    }

    /*
     * Loads the settings files
     */
    public void loadSettings() {
        try {
            // Get dark theme setting
            Scanner scanner = new Scanner(new File("settings/darkTheme.txt"));
            if (scanner.hasNextLine()) {
                settingsDarkTheme = scanner.nextLine().charAt(0) == 'T' ? true : false;
            } else {
                settingsDarkTheme = true;
            }

            // Get music setting
            scanner = new Scanner(new File("settings/music.txt"));
            if (scanner.hasNextLine()) {
                settingsMusic = scanner.nextLine().charAt(0) == 'T' ? true : false;
            } else {
                settingsMusic = true;
            }

            // Get reduce animations setting
            scanner = new Scanner(new File("settings/reduceAnimations.txt"));
            if (scanner.hasNextLine()) {
                settingsReduceAnimations = scanner.nextLine().charAt(0) == 'T' ? true : false;
            } else {
                settingsReduceAnimations = false;
            }

            // Get sound effect setting
            scanner = new Scanner(new File("settings/soundEffects.txt"));
            if (scanner.hasNextLine()) {
                settingsSoundEffects = scanner.nextLine().charAt(0) == 'T' ? true : false;
            } else {
                settingsSoundEffects = true;
            }

            // Get print generated word setting
            scanner = new Scanner(new File("settings/printGeneratedWord.txt"));
            if (scanner.hasNextLine()) {
                settingsPrintGeneratedWord = scanner.nextLine().charAt(0) == 'T' ? true : false;
            } else {
                settingsPrintGeneratedWord = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Updates the settings files
     */
    public void updateSettings() {
        try {
            // Update dark theme setting
            PrintWriter writer = new PrintWriter(new FileWriter("settings/darkTheme.txt", false));
            writer.println(settingsDarkTheme ? "T" : "F");
            writer.close();

            // Update music setting
            writer = new PrintWriter(new FileWriter("settings/music.txt", false));
            writer.println(settingsMusic ? "T" : "F");
            writer.close();

            // Update reduce animation setting
            writer = new PrintWriter(new FileWriter("settings/reduceAnimations.txt"), false);
            writer.println(settingsReduceAnimations ? "T" : "F");
            writer.close();

            // Update sound effects setting
            writer = new PrintWriter(new FileWriter("settings/soundEffects.txt"), false);
            writer.println(settingsSoundEffects ? "T" : "F");
            writer.close();

            // Update print generated word setting
            writer = new PrintWriter(new FileWriter("settings/printGeneratedWord.txt"), false);
            writer.println(settingsPrintGeneratedWord ? "T" : "F");
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Update the theme colors
     */
    public void updateThemeColors() {
        // Update theme colors
        if (settingsDarkTheme) {
            // Set dark theme colors
            backgroundColor = new Color(18, 18, 19);
            textColor = new Color(238, 238, 238);
            buttonColor = new Color(129, 131, 132);
            buttonHoverColor = new Color(109, 111, 112);
            buttonPlayAgainColor = new Color(97, 140, 85);
            buttonPlayAgainHoverColor = new Color(77, 120, 65);
            wordsGridBorderColor = new Color(58, 58, 60);
            wordsGridBorderDeepColor = new Color(86, 87, 88);
            wordsGridExactColor = new Color(97, 140, 85);
            wordsGridCloseColor = new Color(177, 160, 76);
            wordsGridWrongColor = new Color(58, 58, 60);
            popupColor = new Color(216, 218, 220);
            setBackground(backgroundColor);
        } else {
            // Set light theme colors
            backgroundColor = new Color(220, 220, 220);
            textColor = new Color(18, 18, 19);
            buttonColor = new Color(199, 201, 202);
            buttonHoverColor = new Color(169, 171, 172);
            buttonPlayAgainColor = new Color(97, 140, 85);
            buttonPlayAgainHoverColor = new Color(77, 120, 65);
            wordsGridBorderColor = new Color(58, 58, 60);
            wordsGridBorderDeepColor = new Color(86, 87, 88);
            wordsGridExactColor = new Color(117, 160, 105);
            wordsGridCloseColor = new Color(197, 180, 96);
            wordsGridWrongColor = new Color(185, 185, 185);
            popupColor = new Color(18, 18, 19);
            setBackground(backgroundColor);
        } 
    }

    /* 
     * Update the wordsList array 
     * */
    public void loadWordsList() {
        // Enumerate through each gamemode case
        if (gamemode == GAMEMODE_STANDARD) {
            tries = 6;
            wordLength = 5;
            wordsList = new String[14855];
        } else if (gamemode == GAMEMODE_UNLIMITED) {
            tries = 6;
            wordLength = 5;
            wordsList = new String[14855];
        } else if (gamemode == GAMEMODE_3) {
            tries = 6;
            wordLength = 3;
            wordsList = new String[200];
        } else if (gamemode == GAMEMODE_4) {
            tries = 6;
            wordLength = 4;
            wordsList = new String[5525];
        } else if (gamemode == GAMEMODE_5) {
            tries = 6;
            wordLength = 5;
            wordsList = new String[14855];
        } else if (gamemode == GAMEMODE_6) {
            tries = 6;
            wordLength = 6;
            wordsList = new String[22431];
        } else if (gamemode == GAMEMODE_7) {
            tries = 6;
            wordLength = 7;
            wordsList = new String[33305];
        } else if (gamemode == GAMEMODE_8) {
            tries = 6;
            wordLength = 8;
            wordsList = new String[40665];
        } else if (gamemode == GAMEMODE_TIMED_30_SECONDS) {
            tries = 6;
            wordLength = 5;
            wordsList = new String[14855];
            timeCurrent = System.currentTimeMillis();
            timePrevious = timeCurrent;
            timeElapsed = 0;
            timeLimit = 30;
        } else if (gamemode == GAMEMODE_TIMED_45_SECONDS) {
            tries = 6;
            wordLength = 5;
            wordsList = new String[14855];
            timeCurrent = System.currentTimeMillis();
            timePrevious = timeCurrent;
            timeElapsed = 0;
            timeLimit = 45;
        } else if (gamemode == GAMEMODE_TIMED_1_MINUTE) {
            tries = 6;
            wordLength = 5;
            wordsList = new String[14855];
            timeCurrent = System.currentTimeMillis();
            timePrevious = timeCurrent;
            timeElapsed = 0;
            timeLimit = 60;
        }

        // Initialize wordsGrid
        wordsGrid = new Color[tries][wordLength];
        wordsGridBorder = new Color[tries][wordLength];
        for (int i = 0; i < tries; i++) {
            for (int j = 0; j < wordLength; j++) {
                wordsGrid[i][j] = backgroundColor;
                wordsGridBorder[i][j] = wordsGridBorderColor;
            }
        }

        // Initialize wordsEntered
        wordsEntered = new ArrayList<ArrayList<Character>>();
        for (int i = 0; i < tries; i++) {
            wordsEntered.add(new ArrayList<Character>());
        }
        triesLeft = tries;

        // Initialize letterScalings
        letterScalings = new float[wordLength];
        for (int i = 0; i < wordLength; i++) {
            letterScalings[i] = 1.0f;
        }

        // Read in and update the elements of wordsList array
        try {
            Scanner file = new Scanner(new File("words/" + wordLength + ".txt"));
            int i = 0;
            while (file.hasNextLine()) {
                wordsList[i] = file.nextLine();
                i++;
            }
            file.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        // Initialize lettersEntered
        lettersEntered = new int[26];
        lettersEnteredTmp = new int[26];
        for (int i = 0; i < 26; i++) {
            lettersEntered[i] = -1;
            lettersEnteredTmp[i] = -1;
        }
    }

    /*
     * Start the game
     */
    public void startGame() {
        gameOver = false;
        gameWon = false;
        state = STATE_GAME;
        loadWordsList();
        wordGenerated = wordsList[(int) (Math.random() * wordsList.length)];
        if (settingsPrintGeneratedWord) {
            System.out.println("Generated Word: " + wordGenerated);
        }
    }

    /*
     * Draw a switch
     * @param g The graphics instance
     * @param x The centered x position
     * @param y The centered y position
     * @param w The width of the switch
     * @param h The height of the switch
     * @param toggled State of toggle of the switch
     */
    public void drawSwitch(Graphics g, int x, int y, int w, int h, boolean toggled) {
        // Draw round rect background
        drawCenteredRect(
            g,
            toggled ? new Color(97, 140, 85) : new Color(86, 87, 88),
            x, y,
            w, h,
            20, 20
        );
        // Draw circle for the switch
        int d = w < h ? w : h;
        drawCenteredOval(
            g,
            textColor,
            toggled ? x + w / 2 - (int) (d / 2) : x - w / 2 + (int) (d / 2), y,
            (int) (0.8 * d), (int) (0.8 * d)
        );
    }

    /*
     * Draw a centered image
     * @param g The graphics instance
     * @param img The image
     * @param x The center x position
     * @param y The center y position
     */
    public void drawCenteredImage(Graphics g, BufferedImage img, int x, int y) {
        int w = img.getWidth();
        int h = img.getHeight();
        g.drawImage(
            img,
            x - w / 2, y - h / 2,
            null
        );
    }

    /*
     * Resize image
     * @param src The source image
     * @param w The width of the resized image
     * @param h The height of the resized image
     * @return Resized image
     */
    public BufferedImage resizeImage(BufferedImage src, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        int img_w = src.getWidth(), img_h = src.getHeight();

        // Copy the rgb values then rescale it to the new img
        for (int x = 0; x < w; x++) {
            // map x from src after rescale
            int new_x = x * img_w / w;
            for (int y = 0; y < h; y++) {
                // map y from src after rescale
                int new_y = y * img_h / h;
                int rgb = src.getRGB(new_x, new_y);
                img.setRGB(x, y, rgb);
            }
        }
        return img;
    }

    /*
     * Draw a centered string
     * @param g The graphics instance
     * @param string The string to center and draw
     * @param x The x value to draw centered
     * @param y The y value to draw centered
     * @param font The font of the string
     */
    public void drawCenteredString(Graphics g, String string, Font font, Color color, int x, int y) {
        FontMetrics metrics = g.getFontMetrics(font);
        int center_x = x - metrics.stringWidth(string) / 2;
        int center_y = y - metrics.getHeight() / 2 + metrics.getAscent();
        g.setColor(color);
        g.setFont(font);
        g.drawString(string, center_x, center_y);
    }

    /*
     * Draw a left aligned string centered vertically
     * @param g The graphics instance
     * @param string The string to center and draw
     * @param x The x value to draw
     * @param y The y value to draw
     * @param font The font of the string
     */
    public void drawLeftAlignedString(Graphics g, String string, Font font, Color color, int x, int y) {
        FontMetrics metrics = g.getFontMetrics(font);
        g.setColor(color);
        g.setFont(font);
        g.drawString(string, x, (int) (y - metrics.getHeight() / 2 + metrics.getAscent()));
    }
    
    /*
     * Draw a centered string scaled to a certain width and height
     * @param g The graphics instance
     * @param string The string to center and draw
     * @param x The x value to draw centered
     * @param y The y value to draw centered
     * @param font The font of the string
     * @param w The width of the string drawn
     * @param h The height of the string drawn
     */
    public void drawCenteredString(Graphics g, String string, Font font, Color color, int x, int y, int w, int h) {
        g.setFont(font);

        // Get the font metrics
        FontMetrics metrics = g.getFontMetrics();
        float scale_w = (float) w / metrics.stringWidth(string);
        float scale_h = (float) h / metrics.getHeight();

        font = g.getFont().deriveFont(font.getStyle(), AffineTransform.getScaleInstance(scale_w, scale_h));
        g.setColor(color);
        g.setFont(font);

        // Draw the string
        metrics = g.getFontMetrics();
        int center_x = x - metrics.stringWidth(string) / 2;
        int center_y = y - metrics.getHeight() / 2 + metrics.getAscent();
        g.drawString(string, center_x, center_y);
    }

    /*
     * Draw a centered rectangle
     * @param g The graphics instance
     * @param color The color of the rectangle
     * @param x The centered x positition of the rectangle
     * @param y The centered y position of the rectangle
     * @param w The width of the rectangle
     * @param h The height of the rectangle
     */
    public void drawCenteredRect(Graphics g, Color color, int x, int y, int w, int h) {
        g.setColor(color);
        g.fillRect(
            x - w / 2, y - h / 2,
            w, h
        );
    }

    /*
     * Draw a centered round rectangle
     * @param g The graphics instance
     * @param color The color of the rectangle
     * @param x The centered x positition of the rectangle
     * @param y The centered y position of the rectangle
     * @param w The width of the rectangle
     * @param h The height of the rectangle
     * @param arcWidth The arc width of the edges of the round rectangle
     * @param arcHeight The arc height of the edges of the round rectangle
     */
    public void drawCenteredRect(Graphics g, Color color, int x, int y, int w, int h, int arcWidth, int arcHeight) {
        g.setColor(color);
        g.fillRoundRect(
            x - w / 2, y - h / 2, 
            w, h,
            arcWidth, arcHeight
        );
    }

    /*
     * Draw a centered oval
     * @param g The graphics instance
     * @param color The color of the oval
     * @param x The center x-position of the oval
     * @param y The center y-position of the oval
     * @param w The width of the oval
     * @param h The height of the oval
     */
    public void drawCenteredOval(Graphics g, Color color, int x, int y, int w, int h) {
        g.setColor(color);
        g.fillOval(
            x - w / 2, y - h / 2,
            w, h
        );
    }

    /*
     * Draw a button
     * @param g The graphics instance
     * @param string The string for the button
     * @param font The font of the button
     * @param stringColor The string color
     * @param buttonColor The button color
     * @param w The width of the button
     * @param h The height of the button
     * @param x The x position of the button centered
     * @param y The y position of the button centered
     */
    public void drawButton(Graphics g, String string, Font font, Color stringColor, Color buttonColor, int x, int y, int w, int h) {
        drawCenteredRect(
            g, 
            buttonColor, 
            x, y, 
            w, h, 
            10, 10
        );
        drawCenteredString(
            g, 
            string, 
            font, 
            stringColor,
            x, y
        );
    }

    /*
     * Check the bounds of a rectangle
     * @param xs Source x position
     * @param ys Source y position
     * @param x x position of centered rectangle
     * @param y y position of centered rectangle
     * @param w Width of rectangle
     * @param h Height of rectangle
     */
    public boolean checkBounds(int xs, int ys, int x, int y, int w, int h) {
        if (
            xs > x - w / 2 &&
            xs < x + w / 2 &&
            ys > y - h / 2 &&
            ys < y + h / 2
        ) {
            return true;
        }
        return false;
    }

    /*
     * Draw the words grid
     * @param g Graphics instance
     */
    public void drawWordsGrid(Graphics g) {
        // Multiply w and h by letterScalings[j], since letterScalings[i][j] = 1.0f if not chosen anyways

        // Initialize variables
        int x = SCREEN_WIDTH / 2, y = 340;
        int cell_w = 60, cell_h = 60;
        int cell_gap_lr = 5, cell_gap_ud = 5;
        int cell_border_w = 2;

        int grid_h = wordsGrid.length, grid_w = wordsGrid[0].length;
        int start_x = x - (grid_w * (cell_w + cell_gap_lr)) / 2; 
        int start_y = y - (grid_h * (cell_h + cell_gap_ud)) / 2;

        int ly = start_y;

        // Draw the grid
        for (int i = 0; i < grid_h; i++) {
            int lx = start_x;
            for (int j = 0; j < grid_w; j++) {
                // Draw the flipping letters else draw the rest of the grid
                if (flippingLetters && i == tries - triesLeft - 1) {
                    // Draw the letters depending on whether it is the current letter (letter is flipping), behind the flipping letter, or ahead of the flipping letter
                    if (j == flippingLetter) {
                        g.setColor(flippingLetterHeight > 0.0f ? backgroundColor : wordsGrid[i][j]);
                        g.fillRect(
                            lx + cell_gap_lr, ly + cell_gap_ud + (int) (cell_h - cell_h * Math.abs(flippingLetterHeight)) / 2,
                            cell_w, (int) (cell_h * Math.abs(flippingLetterHeight))
                        );
                        g.setColor(flippingLetterHeight > 0.0f ? wordsGridBorderDeepColor : wordsGridBorder[i][j]);
                        for (int k = 0; k < cell_border_w; k++) {
                            g.drawRect(
                                lx + cell_gap_lr + k, ly + cell_gap_ud + (int) (cell_h - cell_h * Math.abs(flippingLetterHeight)) / 2 + k,
                                cell_w - 2 * k, (int) (cell_h * Math.abs(flippingLetterHeight)) - 2 * k
                            );
                        }
                    } else if (j < flippingLetter) {
                        // Draw letters behind the flipping letter
                        g.setColor(wordsGrid[i][j]);
                        g.fillRect(
                            lx + cell_gap_lr, ly + cell_gap_ud,
                            cell_w, cell_h
                        );
                        g.setColor(wordsGridBorder[i][j]);
                        for (int k = 0; k < cell_border_w; k++) {
                            g.drawRect(
                                lx + cell_gap_lr + k, ly + cell_gap_ud + k,
                                cell_w - 2 * k, cell_h - 2 * k
                            );
                        }
                    } else {
                        // Draw the letters ahead of the flipping letter
                        g.setColor(backgroundColor);
                        g.fillRect(
                            lx + cell_gap_lr, ly + cell_gap_ud,
                            cell_w, cell_h
                        );
                        g.setColor(wordsGridBorderDeepColor);
                        for (int k = 0; k < cell_border_w; k++) {
                            g.drawRect(
                                lx + cell_gap_lr + k, ly + cell_gap_ud + k,
                                cell_w - 2 * k, cell_h - 2 * k
                            );
                        }
                    }
                } else {
                    // Draw the rest of the words grid
                    g.setColor(wordsGrid[i][j]);
                    if (i == tries - triesLeft) {
                        // Handle shaking letters effect
                        int width = (int) (letterScalings[j] * cell_w);
                        int height = (int) (letterScalings[j] * cell_h);
                        g.fillRect(
                            lx + cell_gap_lr - (width - cell_w) / 2 + (i == tries - triesLeft ? (int) lettersPositionOffset : 0), ly + cell_gap_ud - (height - cell_h) / 2,
                            width, height
                        );

                        g.setColor(wordsGridBorder[i][j]);
                        for (int k = 0; k < cell_border_w; k++) {
                            width = (int) (letterScalings[j] * (cell_w - 2 * k)); 
                            height = (int) (letterScalings[j] * (cell_h - 2 * k));
                            g.drawRect(
                                lx + cell_gap_lr + k - (width - (cell_w - 2 * k)) / 2 + (i == tries - triesLeft ? (int) lettersPositionOffset : 0), ly + cell_gap_ud + k - (height - (cell_h - 2 * k)) / 2,
                                width, height
                            );
                        }
                    } else {
                        // Handle drawing the rest of the grid
                        g.fillRect(
                            lx + cell_gap_lr + (i == tries - triesLeft ? (int) lettersPositionOffset : 0), ly + cell_gap_ud,
                            cell_w, cell_h
                        );
                        g.setColor(wordsGridBorder[i][j]);
                        for (int k = 0; k < cell_border_w; k++) {
                            g.drawRect(
                                lx + cell_gap_lr + k + (i == tries - triesLeft ? (int) lettersPositionOffset : 0), ly + cell_gap_ud + k,
                                cell_w - 2 * k, cell_h - 2 * k
                            );
                        }
                    }
                }

                // Draw the letters
                String letter = ("" + (i < wordsEntered.size() && j < wordsEntered.get(i).size() ? wordsEntered.get(i).get(j) : "")).toUpperCase();
                Font font = new Font("Arial", Font.BOLD, (int) (30 * (i == tries - triesLeft ? letterScalings[j] : 1.0f)));
                g.setFont(font);
                FontMetrics metrics = g.getFontMetrics();
                drawCenteredString(
                    g,
                    letter,
                    font,
                    textColor,
                    lx + cell_gap_lr + cell_w / 2 + (i == tries - triesLeft ? (int) lettersPositionOffset : 0), ly + cell_gap_ud + cell_h / 2,
                    metrics.stringWidth(letter), (int) (metrics.getHeight() * (i == tries - triesLeft - 1 && j == flippingLetter ? Math.abs(flippingLetterHeight) : 1))
                );
                lx += cell_w + cell_gap_lr;
            }
            ly += cell_h + cell_gap_ud;
        }
    }

    /*
     * Draws the keyboard on the screen
     * @param g The graphics interface
     */
    public void drawKeyboard(Graphics g) {
        // Initialize variables
        int x = SCREEN_WIDTH / 2, y = 560;
        int w = 42, h = 54;
        int gap_lr = 6, gap_ud = 6;

        // Draw the keyboard first row
        String letters = "QWERTYUIOP";
        int lx = x - (10 * (w + gap_lr)) / 2;
        int ly = y;

        for (int i = 0; i < 10; i++) {
            int s = lettersEntered[letters.charAt(i) - 'A'];
            if (s == -1) {
                g.setColor(buttonColor);
            } else if (s == LETTER_EXACT) {
                g.setColor(wordsGridExactColor);
            } else if (s == LETTER_CLOSE) {
                g.setColor(wordsGridCloseColor);
            } else if (s == LETTER_WRONG) {
                g.setColor(wordsGridWrongColor);
            }
            g.fillRoundRect(
                lx, ly,
                w, h,
                10, 10
            );

            g.setColor(textColor);
            String letter = "" + letters.charAt(i);
            Font font = new Font("Arial", Font.BOLD, (int) 20);
            g.setFont(font);
            drawCenteredString(
                g,
                letter,
                font,
                textColor,
                lx + w / 2, ly + h / 2
            );

            lx += w + gap_lr;
        }

        // Draw the keyboard second row
        letters = "ASDFGHJKL";
        lx = x - (9 * (w + gap_lr)) / 2;
        ly += gap_ud + h;
        for (int i = 0; i < 9; i++) {
            int s = lettersEntered[letters.charAt(i) - 'A'];
            if (s == -1) {
                g.setColor(buttonColor);
            } else if (s == LETTER_EXACT) {
                g.setColor(wordsGridExactColor);
            } else if (s == LETTER_CLOSE) {
                g.setColor(wordsGridCloseColor);
            } else if (s == LETTER_WRONG) {
                g.setColor(wordsGridWrongColor);
            }
            g.fillRoundRect(
                lx, ly,
                w, h,
                10, 10
            );

            g.setColor(textColor);
            String letter = "" + letters.charAt(i);
            Font font = new Font("Arial", Font.BOLD, (int) 20);
            g.setFont(font);
            drawCenteredString(
                g,
                letter,
                font,
                textColor,
                lx + w / 2, ly + h / 2
            );

            lx += w + gap_lr;
        }

        // Draw the keyboard third row
        letters = "ZXCVBNM";
        lx = x - (7 * (w + gap_lr)) / 2;
        ly += gap_ud + h;
        for (int i = 0; i < 7; i++) {
            int s = lettersEntered[letters.charAt(i) - 'A'];
            if (s == -1) {
                g.setColor(buttonColor);
            } else if (s == LETTER_EXACT) {
                g.setColor(wordsGridExactColor);
            } else if (s == LETTER_CLOSE) {
                g.setColor(wordsGridCloseColor);
            } else if (s == LETTER_WRONG) {
                g.setColor(wordsGridWrongColor);
            }
            g.fillRoundRect(
                lx, ly,
                w, h,
                10, 10
            );

            g.setColor(textColor);
            String letter = "" + letters.charAt(i);
            Font font = new Font("Arial", Font.BOLD, (int) 20);
            g.setFont(font);
            drawCenteredString(
                g,
                letter,
                font,
                textColor,
                lx + w / 2, ly + h / 2
            );

            lx += w + gap_lr;
        }

        // Draw the delete button
        g.setColor(buttonColor);
        g.fillRoundRect(
            lx, ly,
            2 * w, h,
            10, 10
        );
        Font font = new Font("Arial", Font.BOLD, (int) 20);
        drawCenteredString(
            g,
            "Delete",
            font,
            textColor,
            lx + w, ly + h / 2
        );

        // Draw the enter button
        g.setColor(buttonColor);
        g.fillRoundRect(
            SCREEN_WIDTH - lx - 2 * w - gap_lr, ly,
            2 * w, h,
            10, 10
        );
        drawCenteredString(
            g,
            "Enter",
            font,
            textColor,
            SCREEN_WIDTH - lx - w - gap_lr, ly + h / 2
        );
    }

    /*
     * Checks the word and gives each letter a value of LETTER_EXACT or LETTER_CLOSE or LETTER_WRONG
     * @return Array of entered word status
     */
    public int[] checkWord() {
        // Initialize the status to wrong
        int[] status = new int[wordLength];
        for (int i = 0; i < wordLength; i++) {
            status[i] = LETTER_WRONG;
        }

        // Word check algorithm
        int[] l = new int[26];
        int[] lc = new int[26];
        boolean[] c = new boolean[wordLength];
        for (int i = 0; i < wordLength; i++) {
            l[wordGenerated.charAt(i) - 'a']++;
        }

        // Update the status of the letters in the word
        for (int i = 0; i < wordLength; i++) {
            if (wordsEntered.get(tries - triesLeft).get(i) == wordGenerated.charAt(i)) {
                status[i] = LETTER_EXACT;
                lc[wordGenerated.charAt(i) - 'a']++;
                c[i] = true;
                continue;
            }
        }
        for (int i = 0; i < wordLength; i++) {
            if (c[i]) {
                continue;
            }
            for (int j = 0; j < wordLength; j++) {
                if (wordsEntered.get(tries - triesLeft).get(i) == wordGenerated.charAt(j) && lc[wordGenerated.charAt(j) - 'a'] < l[wordGenerated.charAt(j) - 'a']) {
                    status[i] = LETTER_CLOSE;
                    lc[wordGenerated.charAt(j) - 'a']++;
                    break;
                }
            }
        }

        // Return the status of the letters
        return status;
    }

    /*
     * Calculate the linear interpolation between two points
     * @param a The left endpoint of the interpolation
     * @param b The right endpoint of the interpolation
     * @param t The percentage of a to b, expressed between [0, 1]
     */
    public float lerp(float a, float b, float t) {
        return b * t + a * (1 - t);
    }

    /*
     * Updates the statistics
     */
    public void updateStatistics() {
        try {
            // Get played statistics and write to the file
            Scanner scanner = new Scanner(new File("statistics/played.txt"));
            if (scanner.hasNextLine()) {
                statisticsPlayed = Integer.parseInt(scanner.nextLine());
                statisticsPlayed++;
            } else {
                statisticsPlayed = 1;
            }
            scanner.close();

            PrintWriter writer = new PrintWriter(new FileWriter("statistics/played.txt", false));
            writer.println(statisticsPlayed);
            writer.close();

            // Get win statistics and write to the file
            scanner = new Scanner(new File("statistics/win.txt"));
            if (scanner.hasNextLine()) {
                statisticsWin = Float.parseFloat(scanner.nextLine());
                statisticsWin = ((float) Math.round((statisticsPlayed - 1) * statisticsWin + (gameWon ? 1 : 0)) / statisticsPlayed);
            } else {
                statisticsWin = gameWon ? 1.0f : 0.0f;
            }
            scanner.close();

            writer = new PrintWriter(new FileWriter("statistics/win.txt", false));
            writer.println(statisticsWin);
            writer.close();

            // Get current streak statistics and write to the file
            scanner = new Scanner(new File("statistics/currentStreak.txt"));
            if (scanner.hasNextLine()) {
                statisticsCurrentStreak = Integer.parseInt(scanner.nextLine());
                if (gameWon) {
                    statisticsCurrentStreak++;
                } else {
                    statisticsCurrentStreak = 0;
                }
            } else {
                statisticsCurrentStreak = gameWon ? 1 : 0;
            }
            scanner.close();

            writer = new PrintWriter(new FileWriter("statistics/currentStreak.txt", false));
            writer.println(statisticsCurrentStreak);
            writer.close();

            // Get max streak statistics and write to the file
            scanner = new Scanner(new File("statistics/maxStreak.txt"));
            if (scanner.hasNextLine()) {
                statisticsMaxStreak = Integer.parseInt(scanner.nextLine());
                if (statisticsCurrentStreak > statisticsMaxStreak) {
                    statisticsMaxStreak = statisticsCurrentStreak;
                }
            } else {
                statisticsMaxStreak = statisticsCurrentStreak;
            }

            writer = new PrintWriter(new FileWriter("statistics/maxStreak.txt", false));
            writer.println(statisticsMaxStreak);
            writer.close();

            scanner.close();
        } catch (Exception exception) {
            exception.printStackTrace();
        }

    }

    @Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void keyPressed(KeyEvent e) {
        if (state == STATE_GAME) {
            if (gameOver) {
                return;
            } 
            if (flippingLetters) {
                return;
            }

            // Handle keyboard input
            int keycode = e.getExtendedKeyCode();
            char character = KeyEvent.getKeyText(keycode).charAt(0);
            if (keycode == KeyEvent.VK_BACK_SPACE) {
                // Delete a letter from the words entered
                if (wordsEntered.get(tries - triesLeft).size() > 0) {
                    wordsGridBorder[tries - triesLeft][wordsEntered.get(tries - triesLeft).size() - 1] = wordsGridBorderColor;
                    wordsEntered.get(tries - triesLeft).remove(wordsEntered.get(tries - triesLeft).size() - 1);
                }
            } else if (keycode == KeyEvent.VK_ENTER) {
                // Handle enter key
                if (wordsEntered.get(tries - triesLeft).size() == wordLength) {
                    String w = "";
                    boolean inWordsList = false;
                    for (int i = 0; i < wordLength; i++) {
                        w += wordsEntered.get(tries - triesLeft).get(i);
                    }

                    // Handle if not in the words list
                    for (int i = 0; i < wordsList.length; i++) {
                        if (wordsList[i].equals(w)) {
                            inWordsList = true;
                            break;
                        }
                    }
                    if (!inWordsList) {
                        popup = "Not in words list";
                        popupOpacity = 1.0f;
                        if (!settingsReduceAnimations) {
                            t_popupOpacity = 0.0f;
                            lettersPositionOffset += 0.05f;
                        }
                        return;
                    }

                    // Set the flipping letters animation
                    if (!settingsReduceAnimations) {
                        flippingLetters = true;
                    }

                    // Handle letter statuses and check if the word is correct
                    boolean wordCorrect = true;
                    int[] status = new int[wordLength];
                    status = checkWord();
                    for (int i = 0; i < wordLength; i++) {
                        if (status[i] == LETTER_EXACT) {
                            wordsGrid[tries - triesLeft][i] = wordsGridBorder[tries - triesLeft][i] = wordsGridExactColor;
                        } else if (status[i] == LETTER_CLOSE) {
                            wordsGrid[tries - triesLeft][i] = wordsGridBorder[tries - triesLeft][i] = wordsGridCloseColor;
                            wordCorrect = false;
                        } else if (status[i] == LETTER_WRONG) {
                            wordsGrid[tries - triesLeft][i] = wordsGridBorder[tries - triesLeft][i] = wordsGridWrongColor;
                            wordCorrect = false;
                        }
                    }

                    // Update keyboard letters
                    for (int i = 0; i < wordLength; i++) {
                        int s = lettersEnteredTmp[wordsEntered.get(tries - triesLeft).get(i) - 'a'];
                        lettersEnteredTmp[wordsEntered.get(tries - triesLeft).get(i) - 'a'] = status[i] > s ? status[i] : s;
                    }


                    triesLeft--;

                    // Handle if the word is correct
                    if (wordCorrect) {
                        gameOver = true;
                        gameWon = true;
                        updateStatistics();
                        if (settingsSoundEffects) {
                            try {
                                AudioInputStream sound = AudioSystem.getAudioInputStream(new File("sounds/winSound.wav"));
                                winSound = AudioSystem.getClip();
                                winSound.open(sound);
                                winSound.start();
                            } catch (Exception exception) {
                                exception.printStackTrace();
                            }
                        }
                        return;
                    }

                    // Handle if there are no tries left
                    if (triesLeft == 0) {
                        // Handle if the gamemode is unlimited
                        if (gamemode == GAMEMODE_UNLIMITED) {
                            char[] wordTmp = new char[wordLength];
                            Color[] colorTmp = new Color[wordLength];
                            for (int i = 0; i < wordLength; i++) {
                                wordTmp[i] = wordsEntered.get(tries - 1).get(i);
                                colorTmp[i] = wordsGrid[tries - 1][i];
                            }

                            // Clear the wordsEntered and clear the data in wordsGrid and wordsGridBorder
                            wordsEntered.clear();
                            for (int i = 0; i < tries; i++) {
                                for (int j = 0; j < wordLength; j++) {
                                    wordsGrid[i][j] = backgroundColor;
                                    wordsGridBorder[i][j] = wordsGridBorderColor;
                                }
                            }

                            // Create a new wordsGrid
                            for (int i = 0; i < tries; i++) {
                                wordsEntered.add(new ArrayList<Character>());
                            }
                            // Bring the last row of wordsGrid to the first of the new wordsGrid
                            for (int i = 0; i < wordLength; i++) {
                                wordsEntered.get(0).add(wordTmp[i]);
                                wordsGrid[0][i] = colorTmp[i];
                                wordsGridBorder[0][i] = colorTmp[i];
                            }

                            triesLeft = tries - 1;
                        } else {
                            updateStatistics();
                            gameOver = true;
                            gameWon = false;
                            if (settingsSoundEffects) {
                                try {
                                    AudioInputStream sound = AudioSystem.getAudioInputStream(new File("sounds/loseSound.wav"));
                                    loseSound = AudioSystem.getClip();
                                    loseSound.open(sound);
                                    loseSound.start();
                                } catch (Exception exception) {
                                    exception.printStackTrace();
                                }
                            }
                        }
                    }
                } else {
                    popup = "Not enough letters";
                    popupOpacity = 1.0f;
                    t_popupOpacity = 0.0f;
                    if (!settingsReduceAnimations) {
                        lettersPositionOffset += 0.05f;
                    }
                }
            } else if (Character.isLetter(character)) {
                // If there are enough letters
                if (wordsEntered.get(tries - triesLeft).size() < wordLength) {
                    // Append letter to wordsEntered and create letter bubbling animation
                    wordsEntered.get(tries - triesLeft).add(Character.toLowerCase(character));
                    if (wordsEntered.get(tries - triesLeft).size() > 0) {
                        wordsGridBorder[tries - triesLeft][wordsEntered.get(tries - triesLeft).size() - 1] = wordsGridBorderDeepColor;
                    }
                    if (!settingsReduceAnimations) {
                        letterScalings[wordsEntered.get(tries - triesLeft).size() - 1] += 0.05f;
                    }
                }
            }
        }
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        int mouse_x = e.getX(), mouse_y = e.getY();
        // Set the button state to whatever button the cursor is hovering over
        button = NONE;
        if (state == STATE_MAIN_MENU) {
            if (checkBounds(mouse_x, mouse_y, SCREEN_WIDTH / 2, 400, 200, 50)) {
                button = BUTTON_MAIN_MENU_START;
            } else if (checkBounds(mouse_x, mouse_y, SCREEN_WIDTH / 2, 470, 200, 50)) {
                button = BUTTON_MAIN_MENU_GAMEMODES;
            } else if (checkBounds(mouse_x, mouse_y, SCREEN_WIDTH / 2, 540, 200, 50)) {
                button = BUTTON_MAIN_MENU_SETTINGS;
            } else if (checkBounds(mouse_x, mouse_y, SCREEN_WIDTH / 2, 610, 200, 50)) {
                button = BUTTON_MAIN_MENU_RULES;
            }
        } else if (state == STATE_GAME) {
            if (!gameOver) {
                if (checkBounds(mouse_x, mouse_y, 50, 30, 70, 30)) {
                    button = BUTTON_BACK;
                }
            } else {
                if (checkBounds(mouse_x, mouse_y, SCREEN_WIDTH / 2 - 55, 640, 120, 30)) {
                    button = BUTTON_PLAY_AGAIN;
                } else if (checkBounds(mouse_x, mouse_y, SCREEN_WIDTH / 2 + 55, 640, 70, 30)) {
                    button = BUTTON_BACK;
                }
            } 
        } else if (state == STATE_GAMEMODES_MENU) {
            if (checkBounds(mouse_x, mouse_y, 50, 30, 70, 30)) {
                button = BUTTON_BACK;
            } else if (checkBounds(mouse_x, mouse_y, SCREEN_WIDTH / 2, 220, 200, 50)) {
                button = BUTTON_GAMEMODES_MENU_STANDARD;
            } else if (checkBounds(mouse_x, mouse_y, SCREEN_WIDTH / 2, 280, 200, 50)) {
                button = BUTTON_GAMEMODES_MENU_UNLIMITED;
            } else if (checkBounds(mouse_x, mouse_y, SCREEN_WIDTH / 2 - 200 - 10, 340, 200, 50)) {
                button = BUTTON_GAMEMODES_MENU_3;
            } else if (checkBounds(mouse_x, mouse_y, SCREEN_WIDTH / 2, 340, 200, 50)) {
                button = BUTTON_GAMEMODES_MENU_4;
            } else if (checkBounds(mouse_x, mouse_y, SCREEN_WIDTH / 2 + 200 + 10, 340, 200, 50)) {
                button = BUTTON_GAMEMODES_MENU_5;
            } else if (checkBounds(mouse_x, mouse_y, SCREEN_WIDTH / 2 - 200 - 10, 340 + 50 + 10, 200, 50)) {
                button = BUTTON_GAMEMODES_MENU_6;
            } else if (checkBounds(mouse_x, mouse_y, SCREEN_WIDTH / 2, 340 + 50 + 10, 200, 50)) {
                button = BUTTON_GAMEMODES_MENU_7;
            } else if (checkBounds(mouse_x, mouse_y, SCREEN_WIDTH / 2 + 200 + 10, 340 + 50 + 10, 200, 50)) {
                button = BUTTON_GAMEMODES_MENU_8;
            } else if (checkBounds(mouse_x, mouse_y, SCREEN_WIDTH / 2 - 200 - 10, 400 + 50 + 10, 200, 50)) {
                button = BUTTON_GAMEMODES_MENU_TIMED_30_SECONDS;
            } else if (checkBounds(mouse_x, mouse_y, SCREEN_WIDTH / 2, 400 + 50 + 10, 200, 50)) {
                button = BUTTON_GAMEMODES_MENU_TIMED_45_SECONDS;
            } else if (checkBounds(mouse_x, mouse_y, SCREEN_WIDTH / 2 + 200 + 10, 400 + 50 + 10, 200, 50)) {
                button = BUTTON_GAMEMODES_MENU_TIMED_1_MINUTE;
            }
        } else if (state == STATE_SETTINGS_MENU) {
            if (checkBounds(mouse_x, mouse_y, 50, 30, 70, 30)) {
                button = BUTTON_BACK;
            } else if (checkBounds(mouse_x, mouse_y, (int) (SCREEN_WIDTH * 0.75), 300, 35, 20)) {
                button = BUTTON_SETTINGS_DARK_THEME;
            } else if (checkBounds(mouse_x, mouse_y, (int) (SCREEN_WIDTH * 0.75), 360, 35, 20)) {
                button = BUTTON_SETTINGS_MUSIC;
            } else if (checkBounds(mouse_x, mouse_y, (int) (SCREEN_WIDTH * 0.75), 420, 35, 20)) {
                button = BUTTON_SETTINGS_REDUCE_ANIMATIONS;
            } else if (checkBounds(mouse_x, mouse_y, (int) (SCREEN_WIDTH * 0.75), 480, 35, 20)) {
                button = BUTTON_SETTINGS_SOUND_EFFECTS;
            } else if (checkBounds(mouse_x, mouse_y, (int) (SCREEN_WIDTH * 0.75), 540, 35, 20)) {
                button = BUTTON_SETTINGS_PRINT_GENERATED_WORD;
            }
        } else if (state == STATE_RULES_MENU) {
            if (checkBounds(mouse_x, mouse_y, 50, 30, 70, 30)) {
                button = BUTTON_BACK;
            }
        }
    }

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
        int mouse_x = e.getX(), mouse_y = e.getY();
        // Handle mouse presses in different states depending on what button the mouse is hovering over
        if (state == STATE_MAIN_MENU) {
            if (button == BUTTON_MAIN_MENU_START) {
                popupOpacity = 0.0f;
                t_popupOpacity = 0.0f;
                startGame();
            } else if (button == BUTTON_MAIN_MENU_GAMEMODES) {
                state = STATE_GAMEMODES_MENU;
            } else if (button == BUTTON_MAIN_MENU_SETTINGS) {
                state = STATE_SETTINGS_MENU;
            } else if (button == BUTTON_MAIN_MENU_RULES) {
                state = STATE_RULES_MENU;
            }
        } else if (state == STATE_GAME) {
            if (button == BUTTON_BACK) {
                state = STATE_MAIN_MENU;
            } else if (button == BUTTON_PLAY_AGAIN) {
                startGame();
            }
        } else if (state == STATE_GAMEMODES_MENU) {
            if (button == BUTTON_BACK) {
                state = STATE_MAIN_MENU;
            } else if (button == BUTTON_GAMEMODES_MENU_STANDARD) {
                gamemode = GAMEMODE_STANDARD;
                state = STATE_MAIN_MENU;
            } else if (button == BUTTON_GAMEMODES_MENU_UNLIMITED) {
                gamemode = GAMEMODE_UNLIMITED;
                state = STATE_MAIN_MENU;
            } else if (button == BUTTON_GAMEMODES_MENU_3) {
                gamemode = GAMEMODE_3;
                state = STATE_MAIN_MENU;
            } else if (button == BUTTON_GAMEMODES_MENU_4) {
                gamemode = GAMEMODE_4;
                state = STATE_MAIN_MENU;
            } else if (button == BUTTON_GAMEMODES_MENU_5) {
                gamemode = GAMEMODE_5;
                state = STATE_MAIN_MENU;
            } else if (button == BUTTON_GAMEMODES_MENU_6) {
                gamemode = GAMEMODE_6;
                state = STATE_MAIN_MENU;
            } else if (button == BUTTON_GAMEMODES_MENU_7) {
                gamemode = GAMEMODE_7;
                state = STATE_MAIN_MENU;
            } else if (button == BUTTON_GAMEMODES_MENU_8) {
                gamemode = GAMEMODE_8;
                state = STATE_MAIN_MENU;
            } else if (button == BUTTON_GAMEMODES_MENU_TIMED_30_SECONDS) {
                gamemode = GAMEMODE_TIMED_30_SECONDS;
                state = STATE_MAIN_MENU;
            } else if (button == BUTTON_GAMEMODES_MENU_TIMED_45_SECONDS) {
                gamemode = GAMEMODE_TIMED_45_SECONDS;
                state = STATE_MAIN_MENU;
            } else if (button == BUTTON_GAMEMODES_MENU_TIMED_1_MINUTE) {
                gamemode = GAMEMODE_TIMED_1_MINUTE;
                state = STATE_MAIN_MENU;
            }
        } else if (state == STATE_SETTINGS_MENU) {
            if (button == BUTTON_BACK) {
                state = STATE_MAIN_MENU;
            } else if (button == BUTTON_SETTINGS_DARK_THEME) {
                settingsDarkTheme = settingsDarkTheme ? false : true;
                updateThemeColors();
                updateSettings();
            } else if (button == BUTTON_SETTINGS_MUSIC) {
                settingsMusic = settingsMusic ? false : true;
                if (settingsMusic) {
                    music.start();
                    music.loop(Clip.LOOP_CONTINUOUSLY);
                } else {
                    music.stop();
                }
                updateSettings();
            } else if (button == BUTTON_SETTINGS_REDUCE_ANIMATIONS) {
                settingsReduceAnimations = settingsReduceAnimations ? false : true;
                updateSettings();
            } else if (button == BUTTON_SETTINGS_SOUND_EFFECTS) {
                settingsSoundEffects = settingsSoundEffects ? false : true;
                updateSettings();
            } else if (button == BUTTON_SETTINGS_PRINT_GENERATED_WORD) {
                settingsPrintGeneratedWord = settingsPrintGeneratedWord ? false : true;
                updateSettings();
            }
        } else if (state == STATE_RULES_MENU) {
            if (button == BUTTON_BACK) {
                state = STATE_MAIN_MENU;
            }
        }
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

    // The main program
    public static void main(String args[]) {
        // Set up graphics frame and panel
        JFrame frame = new JFrame("Wordle+");
        WordlePlus panel = new WordlePlus();
        frame.add(panel);
        frame.addKeyListener(panel);
        frame.setVisible(true);
        frame.pack();
        frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
    }
}
