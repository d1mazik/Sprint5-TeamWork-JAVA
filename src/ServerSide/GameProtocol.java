package ServerSide;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class GameProtocol {

    private enum state {
        WAITING_FOR_PLAYERS,
        START_GAME,
        SEND_CATEGORIES,
        SEND_QUESTIONS,
        WAIT_FOR_NEXT_QUESTION,
        RECEIVE_ANSWERS,
        SWAP_PLAYER,
        WAIT_FOR_OPPONENT,
        END_GAME,
    }

    DAO database = new DAO();

    ServerSidePlayer player1, player2;
    ServerSidePlayer currentPlayer, notCurrentPlayer;
    state currentState = state.WAITING_FOR_PLAYERS;
    Category currentCategory;

    //TODO: Bind till properties.
    int questionsPerRound = 2;
    int roundCounter = 0;
    //TODOL: Bind till properties.
    int allowedRounds = 3;

    //TODO: lägg till under swap
    int player1RoundScore;
    int player1TotalScore;

    int player2RoundScore;
    int player2TotalScore;
    int turnCounter = 0;
    int allowedNumberofCategories = 3;


    int currentQuestionIndex = 0;

    public void receive(Object fromUser) throws IOException {
        if (fromUser instanceof Category chosenCategory) {
            //RECEIVE CATEGORIES
            this.currentCategory = chosenCategory;
            currentState = state.SEND_QUESTIONS;
        } else if (fromUser instanceof Integer selectedAnswer) {
            validateAnswer(selectedAnswer);
        }

        try {
            processState();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public int[] getFinalScores(){
        return new int[]{player1.correctAnswers, player2.correctAnswers};
    }
    private void validateAnswer(Integer selectedAnswer) throws IOException {
        Integer correctAnswerIndex =
                currentCategory.getQuestionPackage().get(currentQuestionIndex).getIndexOfCorrectAnswer();
        if (correctAnswerIndex.equals(selectedAnswer)) {
            currentPlayer.incrementCorrectAnswers();
        }
        currentQuestionIndex++;
        currentState = state.WAIT_FOR_NEXT_QUESTION;
        currentPlayer.oos.writeObject(correctAnswerIndex);
    }

    public void setPlayer1(ServerSidePlayer player1) throws IOException {
        this.player1 = player1;
        player1.start();
    }

    public void setPlayer2(ServerSidePlayer player2) throws IOException {
        this.player2 = player2;
        player2.start();
    }

    public void processState() throws IOException, InterruptedException {
        if (currentState == state.WAITING_FOR_PLAYERS) {
            waitForPlayers();
            //waitForPlayers();
        } else if (currentState == state.START_GAME) {
            startGame();
            currentState = state.SEND_CATEGORIES;
            processState();
        } else if (currentState == state.SEND_CATEGORIES) {
            sendCategories();
        } else if (currentState == state.SEND_QUESTIONS) {
            sendQuestion();
        } else if (currentState == state.WAIT_FOR_NEXT_QUESTION) {
            if (currentQuestionIndex >= questionsPerRound) {
                currentQuestionIndex = 0;
                currentState = state.SWAP_PLAYER;
            } else {
                currentState = state.SEND_QUESTIONS;
            }
        } else if (currentState == state.SWAP_PLAYER) {
            turnCounter++;
            System.out.println("Turn counter: " + turnCounter);
            if (turnCounter == 2) {
                //TODO:  nedanstående fungerar inte. Återkoppla till detta senare i mån av tid
//                database.removeUsedQuestionPackageFromCategory(currentCategory, questionsPerRound);
                turnCounter = 0;
                roundCounter++;
                System.out.println("Round counter: " + roundCounter);
                if (roundCounter == allowedRounds) {
                    currentState = state.END_GAME;
                } else {
                    currentState = state.SEND_CATEGORIES;
                }
            } else {
                swapPlayer();
                currentState = state.SEND_QUESTIONS;
            }
            processState();
        } else if (currentState == state.END_GAME){
            player1.oos.writeObject(getFinalScores());
            player2.oos.writeObject(getFinalScores());
        }

    }




//    private void removeUsedQuestionsFromGame() {
//        if (questionsPerRound > 0) {
//            currentCategory.getQuestionPackage().subList(0, questionsPerRound).clear();
//        }
//    }

    private void swapPlayer() throws IOException {
        currentPlayer.oos.writeObject("wait");

        ServerSidePlayer toBeCurrentPlayer = notCurrentPlayer;
        notCurrentPlayer = currentPlayer;
        currentPlayer = toBeCurrentPlayer;
    }

    private void sendQuestion() throws IOException {
        currentPlayer.oos.writeObject(currentCategory.getQuestionPackage().get(currentQuestionIndex));
    }

    private void waitForPlayers() throws IOException, InterruptedException {
        System.out.println("Entered waitForPlayers");
        if (player1 == null) {
            System.out.println("Player one is null");
            return;
        }
        System.out.println("Sent: 'waitForPlayers'");
        player1.oos.writeObject("waitForPlayers");
        if (player2 == null) {
            return;
        }
        player2.oos.writeObject("waitForPlayers");
        System.out.println("SENT WAIT FOR PLAYERS");
        currentState = state.START_GAME;
        processState();
    }

    private void startGame() {
        currentPlayer = player1;
        notCurrentPlayer = player2;
    }

    private void sendCategories() throws IOException {
        List<Category> categoryList = database.categories;
        Collections.shuffle(categoryList);

        Category[] randomCategories = new Category[allowedNumberofCategories];
        for (int i = 0; i < allowedNumberofCategories; i++) {
            randomCategories[i] = categoryList.get(i);
        }

        currentPlayer.oos.writeObject(randomCategories);
        notCurrentPlayer.oos.writeObject("wait");
    }

}