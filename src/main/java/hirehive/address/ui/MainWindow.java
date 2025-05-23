package hirehive.address.ui;

import static hirehive.address.logic.Messages.MESSAGE_DATA_SAVED;
import static hirehive.address.logic.Messages.MESSAGE_EMPTY_ADDRESS_BOOK;
import static hirehive.address.logic.Messages.MESSAGE_LOAD_SUCCESS;
import static hirehive.address.logic.Messages.MESSAGE_SAMPLE_ADDRESS_BOOK;

import java.util.logging.Logger;

import hirehive.address.commons.core.GuiSettings;
import hirehive.address.commons.core.LogsCenter;
import hirehive.address.logic.Logic;
import hirehive.address.logic.commands.CommandResult;
import hirehive.address.logic.commands.ExitCommand;
import hirehive.address.logic.commands.ListCommand;
import hirehive.address.logic.commands.exceptions.CommandException;
import hirehive.address.logic.parser.exceptions.ParseException;
import hirehive.address.model.AddressBook;
import hirehive.address.model.ReadOnlyAddressBook;
import hirehive.address.model.util.SampleDataUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * The Main Window. Provides the basic application layout containing
 * a menu bar and space where other JavaFX elements can be placed.
 */
public class MainWindow extends UiPart<Stage> {

    private static final String FXML = "MainWindow.fxml";

    private final Logger logger = LogsCenter.getLogger(getClass());

    private Stage primaryStage;
    private Logic logic;

    // Independent Ui parts residing in this Ui container
    private PersonListPanel personListPanel;
    private ResultDisplay resultDisplay;
    private HelpWindow helpWindow;
    private NoteWindow noteWindow;

    @FXML
    private StackPane commandBoxPlaceholder;

    @FXML
    private MenuItem helpMenuItem;

    @FXML
    private StackPane personListPanelPlaceholder;

    @FXML
    private StackPane resultDisplayPlaceholder;

    @FXML
    private StackPane statusbarPlaceholder;

    @FXML
    private Label contactCountLabel;

    /**
     * Creates a {@code MainWindow} with the given {@code Stage} and {@code Logic}.
     */
    public MainWindow(Stage primaryStage, Logic logic) {
        super(FXML, primaryStage);

        // Set dependencies
        this.primaryStage = primaryStage;
        this.logic = logic;

        // Configure the UI
        setWindowDefaultSize(logic.getGuiSettings());

        setAccelerators();

        helpWindow = new HelpWindow();
        noteWindow = new NoteWindow();
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    private void setAccelerators() {
        setAccelerator(helpMenuItem, KeyCombination.valueOf("F1"));
    }

    /**
     * Sets the accelerator of a MenuItem.
     * @param keyCombination the KeyCombination value of the accelerator
     */
    private void setAccelerator(MenuItem menuItem, KeyCombination keyCombination) {
        menuItem.setAccelerator(keyCombination);

        /*
         * TODO: the code below can be removed once the bug reported here
         * https://bugs.openjdk.java.net/browse/JDK-8131666
         * is fixed in later version of SDK.
         *
         * According to the bug report, TextInputControl (TextField, TextArea) will
         * consume function-key events. Because CommandBox contains a TextField, and
         * ResultDisplay contains a TextArea, thus some accelerators (e.g F1) will
         * not work when the focus is in them because the key event is consumed by
         * the TextInputControl(s).
         *
         * For now, we add following event filter to capture such key events and open
         * help window purposely so to support accelerators even when focus is
         * in CommandBox or ResultDisplay.
         */
        getRoot().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getTarget() instanceof TextInputControl && keyCombination.match(event)) {
                menuItem.getOnAction().handle(new ActionEvent());
                event.consume();
            }
        });
    }

    /**
     * Fills up all the placeholders of this window.
     */
    void fillInnerParts() {
        personListPanel = new PersonListPanel(logic.getFilteredPersonList());
        personListPanelPlaceholder.getChildren().add(personListPanel.getRoot());

        updateContactCount();

        resultDisplay = new ResultDisplay();
        resultDisplayPlaceholder.getChildren().add(resultDisplay.getRoot());

        loadAddressBookMessage();

        StatusBarFooter statusBarFooter = new StatusBarFooter(logic.getAddressBookFilePath());
        statusbarPlaceholder.getChildren().add(statusBarFooter.getRoot());

        CommandBox commandBox = new CommandBox(this::executeCommand);
        commandBoxPlaceholder.getChildren().add(commandBox.getRoot());
    }

    /**
     * Sets the default size based on {@code guiSettings}.
     */
    private void setWindowDefaultSize(GuiSettings guiSettings) {
        primaryStage.setHeight(guiSettings.getWindowHeight());
        primaryStage.setWidth(guiSettings.getWindowWidth());
        if (guiSettings.getWindowCoordinates() != null) {
            primaryStage.setX(guiSettings.getWindowCoordinates().getX());
            primaryStage.setY(guiSettings.getWindowCoordinates().getY());
        }
    }

    /**
     * Opens the help window or focuses on it if it's already opened.
     */
    @FXML
    public void handleHelp() {
        if (!helpWindow.isShowing()) {
            helpWindow.show();
        } else {
            helpWindow.focus();
        }
    }

    /**
     * Opens the note window or focuses on it if it's already opened.
     */
    @FXML
    public void handleNote() {
        if (!noteWindow.isShowing()) {
            noteWindow.show();
        } else {
            noteWindow.focus();
        }
    }

    void show() {
        primaryStage.show();
    }

    /**
     * Closes the application.
     */
    @FXML
    private void handleExit() {
        GuiSettings guiSettings = new GuiSettings(primaryStage.getWidth(), primaryStage.getHeight(),
                (int) primaryStage.getX(), (int) primaryStage.getY());
        logic.setGuiSettings(guiSettings);
        noteWindow.hide();
        helpWindow.hide();
        primaryStage.hide();
    }

    public PersonListPanel getPersonListPanel() {
        return personListPanel;
    }

    /**
     * Executes the command and returns the result.
     *
     * @see Logic#execute(String)
     */
    private CommandResult executeCommand(String commandText) throws CommandException, ParseException {
        try {
            CommandResult commandResult = logic.execute(commandText);
            logger.info("Result: " + commandResult.getFeedbackToUser());
            String userFeedback = commandResult.getFeedbackToUser();
            // if command has edited the applicant book in some way
            if (commandResult.isChange()) {
                userFeedback += MESSAGE_DATA_SAVED;
            }
            resultDisplay.setFeedbackToUser(userFeedback);
            noteWindow.setNote(logic);

            updateContactCount();

            if (commandResult.isShowHelp()) {
                handleHelp();
            }

            if (commandResult.isExit()) {
                handleExit();
            }

            if (commandResult.isShowNote()) {
                handleNote();
            }

            return commandResult;
        } catch (CommandException | ParseException e) {
            logger.info("An error occurred while executing command: " + commandText);
            resultDisplay.setFeedbackToUser(e.getMessage());
            updateContactCount();
            throw e;
        }
    }

    /**
     * Displays status of addressBook in the results box in the GUI upon loading.
     */
    private void loadAddressBookMessage() {
        ReadOnlyAddressBook currentAddressBook = logic.getAddressBook();
        // Data file could not be read, loads empty AddressBook instead
        if (currentAddressBook.equals(new AddressBook())) {
            resultDisplay.setFeedbackToUser(MESSAGE_EMPTY_ADDRESS_BOOK);
        // Data file does not exist, load sample AddressBook instead
        } else if (currentAddressBook.equals(SampleDataUtil.getSampleAddressBook())) {
            resultDisplay.setFeedbackToUser(MESSAGE_SAMPLE_ADDRESS_BOOK);
        } else {
            // Data file loaded successfully
            resultDisplay.setFeedbackToUser(MESSAGE_LOAD_SUCCESS);
        }
    }

    private void updateContactCount() {
        int count = logic.getFilteredPersonListSize();
        contactCountLabel.setText("Total contacts displayed: " + count);
    }
}
