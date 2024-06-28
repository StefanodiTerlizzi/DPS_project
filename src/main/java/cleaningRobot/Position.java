package cleaningRobot;

import com.google.gson.annotations.SerializedName;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement

public class Position implements Comparable<Position> {
    private int row;

    private int col;

    public Position() {}

    public Position(int row, int col) {
        this.row = row;
        this.col = col;
    }

    public int getRow() {return row;}

    public int getCol() {return col;}

    public void setCol(int col) {
        this.col = col;
    }

    public void setRow(int row) {
        this.row = row;
    }

    @Override
    public boolean equals(Object other) {
        Position p = (other instanceof Position) ? ((Position) other) : null;
        if (p==null) return false;
        return p.getCol() == this.getCol() && this.getRow() == p.getRow();
    }



    @Override
    public String toString() {
        return "Position{" +
                "row=" + row +
                ", col=" + col +
                '}';
    }

    @Override
    public int compareTo(Position other) {
        if (this.row!=other.row) {
            return this.row - other.row;
        } else {
            return this.col - other.col;
        }
    }
}
