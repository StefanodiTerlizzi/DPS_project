package admServer.Model.greenfield;

import cleaningRobot.CleaningRobot;
import cleaningRobot.Position;
import utils.Tuple;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;

public class MatrixGUI extends JFrame {
    private ArrayList<Tuple.TwoTuple<Position, ArrayList<CleaningRobot>>> matrix;
    private DefaultTableModel tableModel;
    private JTable table;

    public MatrixGUI(ArrayList<Tuple.TwoTuple<Position, ArrayList<CleaningRobot>>> matrix) {
        this.matrix = matrix;
        initialize();
    }

    private void initialize() {
        // Set up the frame
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Matrix GUI");
        setSize(400, 300);

        // Create a table model with column names
        String[] columnNames = {"Position", "Robots"};
        tableModel = new DefaultTableModel(columnNames, 0);

        // Create a table using the table model
        table = new JTable(tableModel);

        // Add the table to a scroll pane
        JScrollPane scrollPane = new JScrollPane(table);

        // Add the scroll pane to the frame
        getContentPane().add(scrollPane, BorderLayout.CENTER);

        // Display the frame
        //setVisible(true);
    }

    public void updateMatrix(ArrayList<Tuple.TwoTuple<Position, ArrayList<CleaningRobot>>> updatedMatrix) {
        // Update the matrix data
        this.matrix = updatedMatrix;

        // Clear the table
        tableModel.setRowCount(0);

        // Add updated data to the table
        for (Tuple.TwoTuple<Position, ArrayList<CleaningRobot>> tuple : matrix) {
            Object[] rowData = {tuple.one, tuple.two};
            tableModel.addRow(rowData);
        }
    }
}
