package application.wan.ndd.verifier.apkeep.core;

public class ChangeItemBDD {
	public String from_port;
	public String to_port;
	public int delta;

	public ChangeItemBDD(String port1, String port2, int packets) {
		from_port = port1;
		to_port = port2;
		delta = packets;
	}

	public String toString() {
		return from_port + "->" + to_port + "/" + delta;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof ChangeItemBDD) {
			// System.out.println("in judge!!!!!!!!!!!!!!!");
			// System.out.println(o);
			ChangeItemBDD another = (ChangeItemBDD) o;
			if (!another.from_port.equals(from_port))
				return false;
			if (!another.to_port.equals(to_port))
				return false;
			if (another.delta != delta)
				return false;
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return from_port.hashCode() + to_port.hashCode() + Integer.hashCode(delta);
	}
}
