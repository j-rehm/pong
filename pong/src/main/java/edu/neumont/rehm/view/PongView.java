package edu.neumont.rehm.view;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.net.URL;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The View/Controller that holds all logic, including:
 * Drawing the board,
 * Ball/Paddle position and movement,
 * Collision detection/action, etc.
 */
public class PongView {

    /**
     * The Canvas.
     */
    public Canvas canvas;
    private Timeline timeline;
    private GraphicsContext g;
    private Stage stage;

    private Color gameColor = Color.LIGHTGRAY, background = Color.color(0.07, 0.07, 0.1, 1); // white, limegreen; beige, black; steelblue, cadetblue
    private final double WIDTH = 800, HEIGHT = 600; // 800x600; 1600x900; When changing width/height be sure to make the corresponding changes in the fxml
    private double buffer = 20, textBuffer = 27, textPadding = 15; // 20, 27, 15
    private double textWidth = 40, textHeight = 65; // 15 + 10 + 15 (40), 15 + 10 + 15 + 10 + 15 (65)

    private int playerLScore = 0, playerRScore = 0; // 0, 0
    private int counter = 0, rateInMs = 5; // 0, 20
    private final int WIN_CONDITION = 11; // 11
    private boolean pause = false, lobbyMode = true, indicators = true; // false, true

    private final double padSpd = 0.6 * rateInMs, ballSpd = padSpd / 2; // 0.6
    private final double padW = 14, padH = padW * 5; // 14, 12; 70, 60
    private double padLX = buffer, padLY = HEIGHT/2 - padH/2;
    private double padRYV = 0, padLYV = 0; // 0, 0
    private double padRX = WIDTH-buffer-padW, padRY = HEIGHT/2 - padH/2;

    private final double ballW = padW * 0.9, ballH = ballW; // 14, ballW
    private double ballX = WIDTH/2 - ballW/2, ballY = HEIGHT/2 - ballH/2;
    private double ballXV = 0, ballYV = 0, ballV = 0; // 0, 0, 0
    private double vAdd = padSpd/10, vDir = getStartingDirection(); // 0.3

    private double ballYC = ballY + ballH/2, ballXC = ballX + ballW/2;
    private double padRC = padRY + padH/2, padLC = padLY + padH/2;
    private int collisionIndex = 0; // should only ever be -1, 0, or 1

    private double botSpd = padSpd * 0.8, botRange = padH/2 * 0.80; // padSpd * 0.8, padH/2 * 0.4; 0.3
    private double botX = padLX + padW, botY = ballYC;
    private boolean botActive = false, oldBot = false, dot = false;

    private URL blip, score, opponentScore;


//    private URL blip = new URL(new File(audio_dir + "blip.mp3").toURI().toString()),
//            score = new Media(new File(audio_dir + "score.mp3").toURI().toString()),
//            opponentScore = new Media(new File(audio_dir + "opponent_score.mp3").toURI().toString()),
//            tap = new Media(new File(audio_dir + "tap.mp3").toURI().toString()),
//            score2 = new Media(new File(audio_dir + "score2.mp3").toURI().toString());

    /**
     * Initialize the stage and set the Timeline
     *
     * @param stage the stage to set
     */
    public void init(Stage stage) {
        initSounds();
        this.stage = stage;
//        stage.setWidth(WIDTH);
//        stage.setHeight(HEIGHT);
        stage.setTitle("Pong");
        this.stage.getIcons().add(new Image(this.getClass().getClassLoader().getResourceAsStream("icon.png")));
        stage.setResizable(false);
        stage.show();
        stage.centerOnScreen();
        resetBall();

        timeline = new Timeline(new KeyFrame(Duration.millis(rateInMs), e -> run())); // 20 ms
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void initSounds() {
        try {
            blip = this.getClass().getClassLoader().getResource("blip.mp3");
            score = this.getClass().getClassLoader().getResource("score.mp3");
            opponentScore = this.getClass().getClassLoader().getResource("opponent_score.mp3");
        } catch(Exception ex) {
            ex.printStackTrace();
        }
    }

    private void run() {
        counter++;
        movement();
        keybinds();
        collisionCheck();
        conditionCheck();
        bot();
//        diagnostics();
        if(rateInMs < 5) {
            if(counter % 20 == 1) {
                draw();
            }
        } else {
            draw();
        }

    }

    private void movement() {
        ballX += ballXV;
        ballY += ballYV;
        padRY += padRYV;
        padLY += padLYV;

        // update variables
        ballYC = ballY + ballH/2;
        ballXC = ballX + ballW/2;
        padLC = padLY + padH/2;
        padRC = padRY + padH/2;
    }

    private void keybinds() {
        stage.getScene().setOnKeyPressed(evt -> {
            switch(evt.getCode()) {
                case W:
                    if(!botActive) {
                        padLYV = padSpd * -1;
                    }
                    break;
                case S:
                    if(!botActive) {
                        padLYV = padSpd;
                    }
                    break;
                case UP:
                    if(!oldBot) {
                        padRYV = padSpd * -1;
                    }
                    break;
                case DOWN:
                    if(!oldBot) {
                        padRYV = padSpd;
                    }
                    break;
                case SPACE:
                    if(lobbyMode) {
                        resetGame();
                    } else {
                        pause();
                    }
                    break;
                case ESCAPE:
                    stage.close();
                    break;
                case B:
                    botActive = !botActive;
                    padLYV = 0;
                    break;
                case N:
                    oldBot = !oldBot;
                    padLYV = 0;
                    break;
                case R:
                    resetBall();
                    break;
                case ENTER:
                    resetGame();
                    break;
                case D:
                    dot = botActive;
                    break;
                case I:
                    indicators = !indicators;
                    break;

            }
        });
        stage.getScene().setOnKeyReleased(evt -> {
            switch(evt.getCode()) {
                case W:
                case S:
                    if(!botActive) {
                        padLYV = 0;
                    }
                    break;
                case UP:
                case DOWN:
                    if(!oldBot) {
                        padRYV = 0;
                    }
                    break;
                default:
            }
        });
    }

    @SuppressWarnings("Duplicates")
    private void collisionCheck() {
        // ceiling/floor collision detection TODO
        padRY = (padRY < 0)? 0 : padRY; // right paddle ceiling
        padRY = (padRY + padH > HEIGHT)? HEIGHT - padH : padRY; // right paddle floor
        padLY = (padLY < 0)? 0 : padLY; // left paddle ceiling
        padLY = (padLY + padH > HEIGHT)? HEIGHT - padH : padLY; // left paddle floor
        if(ballY <= 0 && ballYV < 0) { // ball ceiling
            ballY *= -1; // natural reflection
            ballYV *= -1; // swap direction
            playSound(blip);
        }
        if(ballY + ballH >= HEIGHT && ballYV > 0) { // ball floor
            ballYV *= -1;
            ballY = HEIGHT - ((ballY + ballH) - HEIGHT) - ballH;
            playSound(blip);
        }
        
        // lobby mode collision (no paddles, think VHS logo)
        if(lobbyMode) {
            if(ballX + ballW > WIDTH && ballXV > 0) { // ball right wall
                ballX = WIDTH - ((ballX + ballW) - WIDTH) - ballW;
                ballXV *= -1;
                playSound(blip);
            }
            if(ballX < 0 && ballXV < 0) { // ball left wall
                ballX = ballX * -1;
                ballXV *= -1;
                playSound(blip);
            }
        } else { // BIG else

            // right paddle collision prediction
            if(ballX + ballW >= padRX && ballXV > 0) { // if the ball will cross the right paddle zone in the next frame (paddle face)
                if(ballY + ballH >= padRY && ballY <= padRY + padH && ballX < padRX + padW) { // if the ball will contact the paddle in the next frame (dependent on above if statement)
                    double bPTopDist = (ballY + ballH) - padRY; // distance between top of paddle and bottom of ball
                    double bPFaceDist = (ballX + ballW) - padRX; // distance between left paddle face and right ball face
                    double bPBottDist = (padRY + padH) - ballY; // distance between bottom of paddle and top of ball
                    double overlap = 0, reflection = 0;
                    if(bPFaceDist < bPTopDist &&
                       bPFaceDist < bPBottDist) { // the ball will reflect off the face
                        reflection = (ballYC - padRC);
                        overlap = bPFaceDist;
                        ballX = ballX - (overlap * 2); // sets the position of the ball to be just in contact with the paddle (may make the velocity check obsolete)
                        ballXV = ballV * -1; // changes the direction
                    }
                    ballYV = (padRYV != 0) ? reflection * 0.1 : reflection * 0.03; // resets the velocity
                    ballV += vAdd; // increases the speed
                    playSound(blip);
                }
            }
            if(ballX <= padLX + padW && ballXV < 0) { // if the ball will cross the right paddle zone in the next frame (paddle face)
                if(ballY + ballH >= padLY && ballY <= padLY + padH && ballX > padLX) { // if the ball will contact the paddle in the next frame (dependent on above if statement)
                    double bPTopDist = (ballY + ballH) - padLY; // distance between top of paddle and bottom of ball
                    double bPFaceDist = (padLX + padW) - ballX; // distance between left paddle face and right ball face
                    double bPBottDist = (padLY + padH) - ballY; // distance between bottom of paddle and top of ball
                    double overlap = 0, reflection = 0;
                    if(bPFaceDist < bPTopDist &&
                       bPFaceDist < bPBottDist) { // the ball will reflect off the face
                        reflection = (ballYC - padLC);
                        overlap = bPFaceDist;
                        ballX = ballX - (overlap * 2); // sets the position of the ball to be just in contact with the paddle (may make the velocity check obsolete)
                        ballXV = ballV; // changes the direction
                    }
                    ballYV = (padLYV != 0) ? reflection * 0.1 : reflection * 0.03; // resets the velocity
                    ballV += vAdd; // increases the speed
                    playSound(blip);
                }
            }

            // right paddle face collision detection
//            if (ballX + ballW > padRX &&
//                    ((ballY > padRY && ballY < padRY + padH) ||
//                            (ballY + ballH > padRY && ballY + ballH < padRY + padH)) &&
//                    ballXV > 0) {
//                // ball made contact with right paddle
//                double overlap = (ballX + ballW) - padRX;
//                double reflection = (ballYC - padRC);
//                ballX = ballX - (overlap * 2); // sets the position of the ball to be just in contact with the paddle (may make the velocity check obsolete)
//                ballYV = (padRYV != 0) ? reflection * 0.025 : reflection * 0.01; // resets the velocity
//                ballXV = ballV * -1; // changes the direction
//                ballV += vAdd; // increases the speed
////            playSound(blip);
//            }

            // left paddle face collision detection
//            if (ballX < padLX + padW &&
//                    ((ballY > padLY && ballY < padLY + padH) ||
//                            (ballY + ballH > padLY && ballY + ballH < padLY + padH)) &&
//                    ballXV < 0) {
//                // ball made contact with left paddle face
//                double overlap = (padLX + padW) - ballX;
//                double reflection = (ballYC - padLC);
//                ballX = ballX + (overlap * 2);
//                ballYV = (padLYV != 0) ? reflection * 0.025 : reflection * 0.01;
//                ballXV = ballV;
//                ballV += vAdd;
//            playSound(blip);
//            }
        } // end else
    }

    private void playSound(URL url) {
        MediaPlayer mediaPlayer = new MediaPlayer(new Media(url.toString()));
        mediaPlayer.play();
    }

    private void conditionCheck() {
        // left player scores
        if(ballX >= WIDTH + buffer) {
            playerLScore += 1;
            vDir = 1;
            resetBall();
            playSound(opponentScore);
        }
        // right player scores
        if(ballX + ballW <= 0 - buffer) {
            playerRScore += 1;
            vDir = -1;
            resetBall();
            playSound(score);
        }

        if(ballXV == 0 && ballYV == 0) { // built for legacy sound features
            if(counter == 100) { kickoff(); }
        }

        // declare winner TODO
        if(playerLScore >= WIN_CONDITION || playerRScore >= WIN_CONDITION) {
            lobbyMode = true;
        }
    }

    private void draw() {
        g = canvas.getGraphicsContext2D();
        resetBoard(g, background);
        drawScore(g, gameColor, background);
        drawSplit(g, gameColor);
        drawBall(g, gameColor, background);
        if(!lobbyMode) {
            drawPaddles(g, gameColor);
            if(indicators) {drawIndicators(g);}
        }
        if(dot) {drawDot(g, botX, botY);} // used to visually see where the bot predicts the ball to go
    }

    private void bot() {
        // # VERSION 1 # (on the right) (EASY Difficulty)
        if(oldBot) {
            if(ballYC < padRC - padH * 0.3) {
                padRYV = botSpd * -1;
            } else if(ballYC > padRC + padH * 0.3) {
                padRYV = botSpd;
            } else {
                padRYV = 0;
            }
        }

        // # VERSION 2 # (HARD Difficulty; to make medium remove hardmode ternary)
        if(ballXV < 0 && botActive) {
            if(botY < (padLC - botRange)) {
                padLYV = botSpd * -1;
            } else if(botY > (padLC + botRange)) {
                padLYV = botSpd;
            } else if(botY > (padLC - botRange) && botY < (padLC + botRange)) {
                padLYV = (ballX + ballXV > (padLX + padW))? 0 : ((botY < padLC)? botSpd : botSpd * -1); // hardmode
            }
        } else if(ballXV > 0 && botActive) {
            if(ballYC < padLC - 5) {
                padLYV = botSpd * -1;
            } else if(ballYC > padLC + 5) {
                padLYV = botSpd;
            } else {
                padLYV = 0;
            }
        } else if(botActive) {
            padLYV = 0;
        }
        // do the math
        int safety = 0;
        if(ballXV < 0) {
            double slopeX = (ballYV / ballXV) * (ballX - botX);
            botY = ballYC - slopeX;
            while (botY < 0 || botY > HEIGHT && safety < 20) {
                if(botY > HEIGHT) {
                    botY = HEIGHT - (botY - HEIGHT);
                }
                if(botY < 0) {
                    botY *= -1;
                }
                safety++;
            }
        }
    }

    private void drawIndicators(GraphicsContext g) {
        Color active = Color.RED;
        Color inactive = Color.BLUE; // palevioletred, limegreen, blue
        int diameter = 6;
        int border = 5;
        g.setFill(botActive? active : inactive);
        g.fillRect(border, border, diameter, diameter);
        g.setFill(oldBot? active : inactive);
        g.fillRect(WIDTH - (border + diameter), border, diameter, diameter);
        g.setFill(gameColor);
    }

    private void drawDot(GraphicsContext g, double x, double y) {
        double f = 2;
        double s = 1;
        g.setFill(background);
        g.fillRect(x-(f/2)-s, y-(f/2)-s, s+f+s, s+f+s);
        g.setFill(gameColor);
        g.fillRect(x-(f/2), y-(f/2), f, f);
    }

    private void resetGame() {
        resetBall();
        resetPaddles();
        playerRScore = 0;
        playerLScore = 0;
        lobbyMode = false;
        timeline.play();
    }

    private void resetBall() {
        counter = 0;
        ballX = WIDTH/2 - ballW/2;
        ballY = HEIGHT/2 - ballH/2;
        ballXV = 0;
        ballYV = 0;
        ballV = ballSpd;
    }

    private void resetPaddles() {
        padLY = (HEIGHT/2) - (padH/2);
        padRY = padLY;
    }

    private void kickoff() {
        ballYV += ThreadLocalRandom.current().nextDouble(ballSpd*-1, ballSpd);
        ballXV = ballSpd * vDir;
    }

    /**
     * Picks an integer to multiply with ballXV
     * @return either 1 or -1
     */
    private int getStartingDirection() {
        int result = 0;
        for (int i = 0; i < i + 1 && result == 0; i++) {
            result = new Random().nextInt(3) - 1;
        }
        return result;
    }

    private void drawScore(GraphicsContext g, Color c, Color bg) {
        drawPlayerLScore(g, c, bg);
        drawPlayerRScore(g, c, bg);
    }

    private void drawPlayerLScore(GraphicsContext g, Color c, Color bg) {
        int[] ints = splitInts(playerLScore);
        for(int i = 0; i < ints.length; i++) {
            double x = WIDTH/2 - ((textWidth+textPadding)*ints.length) + textPadding - textBuffer;
            double y = buffer;
            x += i*(textWidth+textPadding);
            drawNumber(g, x, y, ints[i], c, bg);
        }
    }

    private void drawPlayerRScore(GraphicsContext g, Color c, Color bg) {
        int[] ints = splitInts(playerRScore);
        for(int i = 0; i < ints.length; i++) {
            double x = WIDTH/2 + textBuffer;
            x -= ints[0] == 1 ? 25 : 0;
            double y = buffer;
            x += i*(textWidth+textPadding);
            drawNumber(g, x, y, ints[i], c, bg);
        }
    }

    private void pause() {
        if(pause) {
            pause = false;
            timeline.play();
        } else {
            pause = true;
            timeline.pause();
        }
    }

    private void resetBoard(GraphicsContext g, Color background) {
        g.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        g.setFill(background);
        g.fillRect(0,0,canvas.getWidth(), canvas.getHeight());
    }

    private void drawBall(GraphicsContext g, Color color, Color background) {
        g.setFill(background);
        int stroke = 3;
        g.fillRect(ballX-stroke, ballY-stroke, ballW+(stroke*2), ballH+(stroke*2));
        g.setFill(color);
        g.fillRect(ballX, ballY, ballW, ballH);
    }

    private void drawLeftPaddle(GraphicsContext g) {
        g.fillRect(padLX, padLY, padW, padH);
    }

    private void drawRightPaddle(GraphicsContext g) {
        g.fillRect(padRX, padRY, padW, padH);
    }

    private void drawPaddles(GraphicsContext g, Color color) {
        g.setFill(color);
        drawLeftPaddle(g);
        drawRightPaddle(g);
    }

    private void drawSplit(GraphicsContext g, Color color) {
        g.setFill(color);
        double lineWidth = 3; // def 10; fav 3
        double margin = 4; // def 15; fav 10, 8
        int numOfLines = 80; // def 16; fav 40, 80
        double lineLength = (HEIGHT + margin) / numOfLines - margin; // def 23.4375

        for (int i = 0; i < numOfLines; i++) {
            g.fillRect(canvas.getWidth()/2 - lineWidth/2,i*lineLength + i*margin, lineWidth, lineLength);
        }
    }

    private void drawNumber(GraphicsContext g, double x, double y, int number, Color c, Color bg) {
        g.setFill(c);
        g.fillRect(x, y, textWidth, textHeight);
        g.setFill(bg);
        switch (number) {
            case 0:
                g.fillRect(x + 15, y + 15, 10, 35);
                break;
            case 1:
                g.fillRect(x + 0, y + 0, 25, 65);
                break;
            case 2:
                g.fillRect(x + 0, y + 15, 25, 10);
                g.fillRect(x + 15, y + 40, 25, 10);
                break;
            case 3:
                g.fillRect(x + 0, y + 15, 25, 10);
                g.fillRect(x + 0, y + 40, 25, 10);
                break;
            case 4:
                g.fillRect(x + 15, y + 0, 10, 25);
                g.fillRect(x + 0, y + 40, 25, 25);
                break;
            case 5:
                g.fillRect(x + 15, y + 15, 25, 10);
                g.fillRect(x + 0, y + 40, 25, 10);
                break;
            case 6:
                g.fillRect(x + 15, y + 0, 25, 25);
                g.fillRect(x + 15, y + 40, 10, 10);
                break;
            case 7:
                g.fillRect(x + 0, y + 15, 25, 50);
                break;
            case 8:
                g.fillRect(x + 15, y + 15, 10, 10);
                g.fillRect(x + 15, y + 40, 10, 10);
                break;
            case 9:
                g.fillRect(x + 15, y + 15, 10, 10);
                g.fillRect(x + 0, y + 40, 25, 25);
                break;
        }
    }

    private int[] splitInts(int num) {
        String[] intStrings = Integer.toString(num).split("|");
        int[] ints = new int[intStrings.length];
        for(int i = 0; i < intStrings.length; i++) {
            ints[i] = Integer.parseInt(intStrings[i]);
        }
        return ints;
    }

    private void diagnostics() {
        System.out.println(
                "LY: " + padLY + " RY: " + padRY + "\n" +
                        "BX: " + ballX + " BY: " + ballY + "\n" +
                        "XV: " + ballXV + " YV: " + ballYV + "\n"
        );
    }

}
