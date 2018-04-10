package tdg.teide;

import tdg.link_discovery.framework.tools.data_loader.ILoader;
import tdg.link_discovery.framework.tools.data_loader.JenaTDBLoader;

public class DatasetLoader {

	public static void main(String[] args) {
		ILoader loader = new JenaTDBLoader();
		loader.loadDataFromFile("/Users/andrea/Desktop/teide-dumps/dblp-dedup/tdb-data/dblp-dedup", "/Users/andrea/Desktop/teide-dumps/dblp-dedup/dblp-dedup.nt");

	}

}
