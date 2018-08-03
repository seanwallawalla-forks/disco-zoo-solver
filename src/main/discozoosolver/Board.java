package main.discozoosolver;

import main.ui.BoardDisplay;
import main.ui.SolverApp;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Class representing the game board.
 *
 * The board keeps a list of Cells and maintains the set of possible animal candidates based on the current state of
 * the cells and the solver.
 */
public class Board {
    /* The list of current possible candidates */
    private List<Candidate> candidates;
    /* The list of current animals */
    private List<Animal> animals;
    /* The list of grid cells */
    private List<Cell> cells;
    /* The display class linked to this board */
    private BoardDisplay boardDisplay;
    /* The current game location */
    private String location;
    /* The solver which controls the game state */
    private SolverApp solver;

    /**
     * Create a new board. Initialised as a BOARD_SIZE x BOARD_SIZE grid of empty cells.
     * @param solver The solver which controls game state.
     */
    public Board(SolverApp solver) {
        this.solver = solver;
        /* Initialise the list of cells */
        this.cells = new ArrayList<>();
        createCells();
        /* Create the display class and link it */
        this.boardDisplay = new BoardDisplay(solver, this);
        resetBoard();
    }

    /**
     * Set the current game location.
     * @param location the location to set.
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * @return the current game location.
     */
    public String getLocation() {
        return location;
    }

    /**
     * @return the list of cells.
     */
    public List<Cell> getCells() {
        return cells;
    }

    /**
     * @return the cell located at (x, y)
     */
    public Cell getCell(int x, int y) {
        return cells.get(y * Constants.BOARD_SIZE + x);
    }

    /**
     * Creates BOARD_SIZE x BOARD_SIZE empty cells and add them to cells.
     */
    public void createCells() {
        for (int y = 0; y < Constants.BOARD_SIZE; y++) {
            for (int x = 0; x < Constants.BOARD_SIZE; x++) {
                Cell cell = new Cell(x, y, solver);
                cells.add(cell);
            }
        }
    }

    /**
     * @return the display class linked to this board.
     */
    public BoardDisplay getBoardDisplay() {
        return boardDisplay;
    }

    /**
     * Update the display linked to this board.
     */
    public void updateDisplay() {
        boardDisplay.updateDisplay();
    }

    /**
     * Add an animal to the current list of animals.
     * @param animal the animal to add.
     */
    public void addAnimal(Animal animal) {
        animals.add(animal);
    }

    public void generateCandidates() {
        for (Animal animal : animals) {
            int height = animal.getPattern().getHeight();
            int width = animal.getPattern().getWidth();
            for (int y = 0; y <= Constants.BOARD_SIZE - height; y++) {
                for (int x = 0; x <= Constants.BOARD_SIZE - width; x++) {
                    List<Block> blocks = new ArrayList<>();
                    for (Block block : animal.getPattern().getBlocks()) {
                        blocks.add(new Block(x + block.x(), y + block.y()));
                    }
                    candidates.add(new Candidate(animal, blocks));
                }
            }
        }
        generateCellContents();
        checkForKnownCells();
        updatePriorities();
    }

    /**
     * Sets the count of possible candidates and the list of animals which could be in each cell.
     */
    public void generateCellContents() {
        resetCounts();
        resetAnimalsInCells();
        for (Candidate candidate : candidates) {
            for (Block block : candidate.getPosition()) {
                String animal = candidate.getAnimal().getName();
                Cell cell = getCell(block.x(), block.y());
                cell.addAnimal(animal);
                cell.incrementCount();
            }
        }
    }

    private void checkForKnownCells() {
        for (Animal animal : animals) {
            List<Candidate> options = new ArrayList<>();
            String name = animal.getName();
            for (Candidate candidate : candidates) {
                if (candidate.getAnimal().getName().equals(name)) {
                    options.add(candidate);
                }
            }
            //System.out.println(options);
            if (options.size() == 1) {
                setKnownCandidate(options.get(0));
            } else {
                Candidate firstCandidate = options.get(0);
                for (Block block : firstCandidate.getPosition()) {
                    Boolean known = true;
                    for (Candidate candidate : options) {
                        if (!(candidate.getPosition().contains(block))) {
                            known = false;
                        }
                    }
                    // System.out.println(known + " " + block);
                    if (known) {
                        setKnownBlock(block, firstCandidate.getAnimal().getName());
                    }
                }
            }
        }
        generateCellContents();
        updatePriorities();
    }

    /**
     * Sets known status for each block in the candidate.
     * @param candidate The candidate which is known.
     */
    private void setKnownCandidate(Candidate candidate) {
        String animal = candidate.getAnimal().getName();
        for (Block block : candidate.getPosition()) {
            setKnownBlock(block, animal);
        }
    }

    /**
     * Sets the status of the block which is known to contain animal.
     * @param block The block which is known to contain animal.
     * @param animal The animal which block contains.
     */
    private void setKnownBlock(Block block, String animal) {
        Cell cell = getCell(block.x(), block.y());
        if (!cell.getFinalised()) {
            // Remove any candidate using the block
            Predicate<Candidate> candidatePredicate = c -> (c.getPosition().contains(block) ^ c.getAnimal().getName()
                    .equals(animal));
            candidates.removeIf(candidatePredicate);
            // Set cell as known
            cell.setKnown(true);
            cell.setFinalised(false);
        }
    }

    public void confirmHit(Block block, String animal) {
        Predicate<Candidate> candidatePredicate = c -> (c.getPosition().contains(block) ^ c.getAnimal().getName()
                .equals(animal));
        candidates.removeIf(candidatePredicate);
        setFinalised(block);
        generateCellContents();
        checkForKnownCells();
        updatePriorities();

    }

    public void confirmMiss(Block block) {
        Predicate<Candidate> candidatePredicate = c -> c.getPosition().contains(block);
        candidates.removeIf(candidatePredicate);
        setFinalised(block);
        generateCellContents();
        checkForKnownCells();
        updatePriorities();
    }

    public void setFinalised(Block block) {
        Cell cell = getCell(block.x(), block.y());
        cell.setFinalised(true);
        cell.setKnown(false);
    }

    public void resetBoard() {
        resetCells();
        resetCandidates();
        resetAnimals();
    }

    private void resetCells() {
        for (Cell cell : cells) {
            cell.resetCell();
        }
    }

    private void resetCounts() {
        for (Cell cell : cells) {
            cell.resetCount();
        }
    }

    private void resetAnimalsInCells() {
        for (Cell cell : cells) {
            cell.clearAnimals();
        }
    }

    private void resetCellPriorities() {
        for (Cell cell : cells) {
            cell.setPriority(false);
        }
    }

    private void resetCandidates() {
        candidates = new ArrayList<>();
    }

    private void resetAnimals() {
        animals = new ArrayList<>();
    }

    public void updatePriorities() {
        int maxCount = 0;
        for (Cell cell : cells) {
            if (!(cell.getFinalised() || cell.getKnown())) {
                if (cell.getCount() < maxCount) {
                    continue;
                } else if (cell.getCount() > maxCount) {
                    maxCount = cell.getCount();
                    resetCellPriorities();
                }
                cell.setPriority(true);
            }
        }
    }

    public void printCount() {
        StringBuilder sb = new StringBuilder();
        for (Cell cell : cells) {
            sb.append(cell.getCount());
            sb.append(" ");
        }
        sb.append("\n");
        System.out.println(sb.toString());
    }

    public void printPositions() {
        System.out.println(candidates);
    }
}