package offline;


// User class方便于我们做sorting or hashing

public class User implements Comparable<User> {
	private String id; // user id
	// value就是similarity score
	private Double value; // how many matches between this user and the target user  note: 所谓match就是对同一个产品有一样的rating
	
	// Getter and Setter for our two fields: id and value.
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Double getValue() {
		return value;
	}

	public void setValue(Double value) {
		this.value = value;
	}

	// constructor
	public User(String id, Double value) {
		super();
		this.id = id;
		this.value = value;
	}
	
	// calculate hash value, which involves both id and value
	// Note: 其实这个method在我们的程序里未被用到
	@Override
	public int hashCode() {
		final int prime = 31; // prime number
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		User other = (User) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}
	
	// note: Collections.sort()排序是要由compareTo()来决定
	@Override
	public int compareTo(User other) {
		return (int) (other.value - this.value);
	}
}
