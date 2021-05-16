package larc.recommender.data;

import java.util.Arrays;

public class Data {
	private final Instance[] data;
	private final Instance[] transData;
	private String name = "";
	
	public Data(int numRows, int numCols) {
		this.data = new Instance[numRows];
		for (int i = 0; i < numRows; i++) {
			this.data[i] = new Instance();
		}
		this.transData = new Instance[numCols];
		for (int j = 0; j < numCols; j++) {
			this.transData[j] = new Instance();
		}
	}
	public void clear() {
		for (int i = 0; i < this.data.length; i++) {
			this.data[i].clear();
		}
		for (int j = 0; j < this.transData.length; j++) {
			this.transData[j].clear();
		}
	}
	public Instance[] get() {
		return data;
	}
	public Instance[] getTranspose() {
		return transData;
	}
	public Instance row(int row) {
		return this.data[row];
	}
	public Instance col(int col) {
		return this.transData[col];
	}
	public int getNumRows() {
		return this.data.length;
	}
	public int getNumCols() {
		return this.transData.length;
	}
	public String getName() {
		return name;
	}
	public Data setName(String name) {
		this.name = name;
		return this;
	}
	public void set(int row, int col, double value) {
		if (row < 0 && row >= this.data.length) {
			throw new IndexOutOfBoundsException("Invalid row index " + row);
		} else if (col < 0 && col >= this.transData.length) {
			throw new IndexOutOfBoundsException("Invalid column index " + col);
		}
		this.data[row].add(new Feature(col, value));
		this.transData[col].add(new Feature(row, value));
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(data);
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + Arrays.hashCode(transData);
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Data other = (Data) obj;
		return (Arrays.equals(data, other.data) && Arrays.equals(transData, other.transData) && name.equals(other.name));
	}
}
