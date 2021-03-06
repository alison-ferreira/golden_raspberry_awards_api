package db.migration;

import java.io.FileReader;
import java.sql.PreparedStatement;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.texoit.vo.MovieNominationVO;

public class V2__CSV_Unload extends BaseJavaMigration {
	
	private static Logger log = LoggerFactory.getLogger(V2__CSV_Unload.class);
	
	private static final String CSV_FILE_NAME = "movielist.csv";
	private static final String CSV_MOVIE_NOMINATION_YEAR = "year";
	private static final String CSV_MOVIE_NOMINATION_TITLE = "title";
	private static final String CSV_MOVIE_NOMINATION_STUDIOS = "studios";
	private static final String CSV_MOVIE_NOMINATION_PRODUCERS = "producers";
	private static final String CSV_MOVIE_NOMINATION_WINNER = "winner";
	private static final String CSV_TRUE_VALUE = "yes";
	private static final char CSV_DELIMITER = ';';
	private static final int CSV_HEADER_LINES = 1;
	
	private static final String INSERT_MOVIE_NOMINATION = "INSERT INTO movie_nomination (year, title, studios, producers, winner) VALUES (?, ?, ?, ?, ?)";
	private static final String FINAL_MESSAGE = "CSV migration finished with %d/%d rows migrated.";
	
	private static int csvCountRows = 0;
	private static int csvMigratedRows = 0;
	

	@Override
	public void migrate(Context context) throws Exception {
		FileReader csvFile = new FileReader(getClass().getClassLoader().getResource(CSV_FILE_NAME).getPath());
		
		Iterable<CSVRecord> csvRecordList = CSVFormat.EXCEL.withHeader().withDelimiter(CSV_DELIMITER).parse(csvFile);
		
		for(CSVRecord csvRecord : csvRecordList) {
			csvCountRows++;
			
            try (PreparedStatement ps = context.getConnection().prepareStatement(INSERT_MOVIE_NOMINATION)) {
            	MovieNominationVO movieNomination = getMovieNominationFromCSVRecord(csvRecord);
            	
            	int idx = 1;
            	ps.setInt(idx++, movieNomination.getYear());
            	ps.setString(idx++, movieNomination.getTitle());
            	ps.setString(idx++, movieNomination.getStudios());
            	ps.setString(idx++, movieNomination.getProducers());
            	ps.setBoolean(idx++, movieNomination.getWinner());
            	
            	ps.executeUpdate();
        		csvMigratedRows++;
            } catch (Exception e) {
				log.warn(String.format("%s: %s. In the CSV file line: %d.", e.getClass(), e.getMessage(), (csvCountRows + CSV_HEADER_LINES)));
			}
		}
		
		context.getConnection().commit();
		
		log.info(FINAL_MESSAGE.formatted(csvMigratedRows, csvCountRows));
	}

	private MovieNominationVO getMovieNominationFromCSVRecord(CSVRecord csvRecord) {
		MovieNominationVO movieNomination = new MovieNominationVO();
		
		movieNomination.setYear(Integer.parseInt(csvRecord.get(CSV_MOVIE_NOMINATION_YEAR)));
		movieNomination.setTitle(csvRecord.get(CSV_MOVIE_NOMINATION_TITLE));
		movieNomination.setStudios(csvRecord.get(CSV_MOVIE_NOMINATION_STUDIOS));
		movieNomination.setProducers(csvRecord.get(CSV_MOVIE_NOMINATION_PRODUCERS));
		movieNomination.setWinner(CSV_TRUE_VALUE.equalsIgnoreCase(csvRecord.get(CSV_MOVIE_NOMINATION_WINNER)) ? true : false);
		
		return movieNomination;
	}

}
