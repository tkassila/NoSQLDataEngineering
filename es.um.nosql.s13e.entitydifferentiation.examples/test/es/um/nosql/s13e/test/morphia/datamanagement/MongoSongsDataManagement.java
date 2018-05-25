package es.um.nosql.s13e.test.morphia.datamanagement;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import javax.validation.Validation;
import javax.validation.Validator;

import org.bson.types.ObjectId;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.ValidationExtension;

import es.um.nosql.s13e.db.adapters.mongodb.MongoDbAdapter;
import es.um.nosql.s13e.db.adapters.mongodb.MongoDbClient;
import es.um.nosql.s13e.mongosongs.Album;
import es.um.nosql.s13e.mongosongs.Artist;
import es.um.nosql.s13e.mongosongs.Media;
import es.um.nosql.s13e.mongosongs.Prize;
import es.um.nosql.s13e.mongosongs.Rating;
import es.um.nosql.s13e.mongosongs.Review;
import es.um.nosql.s13e.mongosongs.Track;

public class MongoSongsDataManagement
{
  private Morphia morphia;
  private MongoDbClient client;
  private Datastore datastore;
  private String dbName;
  private Validator validator;

  public MongoSongsDataManagement(String ip, String dbName)
  {
    this.dbName = dbName;
    this.morphia = new Morphia();
    this.morphia = morphia.mapPackage("es.um.nosql.s13e.mongosongs");
    new ValidationExtension(this.morphia);
    this.client = MongoDbAdapter.getMongoDbClient(ip);
    this.datastore = this.morphia.createDatastore(this.client, this.dbName);
    this.datastore.ensureIndexes();
    validator = Validation.buildDefaultValidatorFactory().getValidator();
  }

  public void startDemo()
  {
//    fillDbPinkFloyd();
    fillDbPearlJam();
    fillDbMassiveAttack();
  }

  private void fillDbPinkFloyd()
  {
    Artist artist = new Artist(); artist.set_id(new ObjectId().toString()); artist.setName("Pink Floyd"); artist.setStartingYear(1965);

    Track t1 = new Track(); t1.set_id(new ObjectId().toString()); t1.setArtist_id(Arrays.asList(artist)); t1.setLength(13.32); t1.setName("Shine on you crazy diamond (I-V)");
    t1.setGenres(Arrays.asList("Progressive rock")); t1.setPopularity(8.9); t1.setRatings(Arrays.asList(createRating(22.7, 1273)));

    Track t2 = new Track(); t2.set_id(new ObjectId().toString()); t2.setArtist_id(Arrays.asList(artist)); t2.setLength(7.31); t2.setName("Welcome to the machine");
    t2.setGenres(Arrays.asList("Progressive rock", "Electronic rock")); t2.setPopularity(3.8); t2.setRatings(Arrays.asList(createRating(5.3, 531)));

    Track t3 = new Track(); t3.set_id(new ObjectId().toString()); t3.setArtist_id(Arrays.asList(artist)); t3.setLength(5.41); t3.setName("Wish you were here");
    t3.setGenres(Arrays.asList("Vocal")); t3.setPopularity(6.9); t3.setRatings(Arrays.asList(createRating(33.1, 3310)));

    Album a1 = new Album(); a1.set_id(new ObjectId().toString()); a1.setFormats(Arrays.asList("Vinyl", "Album")); a1.setName("Wish you were here");
    a1.setPopularity(7.7); a1.setReleaseYear(1975); a1.setTracks(Arrays.asList(t1, t2, t3)); a1.setAvailability(Arrays.asList("EN", "FR")); a1.setGenre("Progressive rock");
    a1.setReviews(Arrays.asList(
        createReview("Blender", "Excelent", "5/5", url),
        createReview("The Great Rock Discography", "Excelent", "10/10", url),
        createReview("The Rolling Stone Album Guide", "5 stars", ""));
    //TODO: Almost

    artist.setComposedTracks(Arrays.asList(t1, t2, t3, t4, t5, t6)); artist.setLyricsTracks(Arrays.asList(t1, t2, t3, t4, t5, t6)); artist.setAlbums(Arrays.asList(a1, a2));
  }

  private void fillDbMassiveAttack()
  {
    Artist artist = new Artist(); artist.set_id(new ObjectId().toString()); artist.setName("Massive Attack"); artist.setStartingYear(1988);

    Track t1 = new Track(); t1.set_id(new ObjectId().toString()); t1.setArtist_id(Arrays.asList(artist)); t1.setLength(6.19); t1.setName("Angel");
    t1.setGenres(Arrays.asList("Trip hop", "Industrial rock")); t1.setPopularity(9.8); t1.setRatings(Arrays.asList(createRating(18.0, 180)));

    Track t2 = new Track(); t2.set_id(new ObjectId().toString()); t2.setArtist_id(Arrays.asList(artist)); t2.setLength(5.31); t2.setName("Teardrop");
    t2.setGenres(Arrays.asList("Trip hop", "Downtempo")); t2.setPopularity(6.1); t2.setRatings(Arrays.asList(createRating(19.0, 190)));

    Track t3 = new Track(); t3.set_id(new ObjectId().toString()); t3.setArtist_id(Arrays.asList(artist)); t3.setLength(6.06); t3.setName("Dissolved girl");
    t3.setGenres(Arrays.asList("Trip hop")); t3.setPopularity(7.7); t3.setRatings(Arrays.asList(createRating(5.0, 50)));

    Album a1 = new Album(); a1.set_id(new ObjectId().toString()); a1.setFormats(Arrays.asList("LP", "Album")); a1.setName("Mezzanine");
    a1.setPopularity(8.0); a1.setReleaseYear(1998); a1.setTracks(Arrays.asList(t1, t2, t3)); a1.setAvailability(Arrays.asList("ES", "EN", "FR")); a1.setGenre("Trip hop");
    a1.setReviews(Arrays.asList(
        createReview("Barney Hoskyns", "Very good", "3.5/5", "Rolling stone"),
        createReview("Alexis Petridis", "Excelent", 5, Arrays.asList(createMedia("The Guardian", "https://www.theguardian.com/us", "newspaper")))));
    a1.setPrizes(Arrays.asList(
        createPrize("RIAA", 70000, 1998, "Platinum disk", "Australia", null),
        createPrize("SNEP", 243000, 1998, "2x Gold disk", "France", null)));

    Track t4 = new Track(); t4.set_id(new ObjectId().toString()); t4.setArtist_id(Arrays.asList(artist)); t4.setLength(5.14); t4.setName("Karmacoma");
    t4.setGenres(Arrays.asList("Trip hop")); t4.setPopularity(4.5);

    Track t5 = new Track(); t5.set_id(new ObjectId().toString()); t5.setArtist_id(Arrays.asList(artist)); t5.setLength(4.51); t5.setName("Live with me");
    t5.setGenres(Arrays.asList("Trip hop")); t5.setPopularity(5.0);

    Album a2 = new Album(); a2.set_id(new ObjectId().toString()); a2.setFormats(Arrays.asList("Album", "Vinyl")); a2.setName("Collected"); a2.setPopularity(6.6);
    a2.setReleaseYear(2006); a2.setTracks(Arrays.asList(t4, t5, t1)); a2.setAvailability(Arrays.asList("EN", "JP"));
    a2.setGenres(Arrays.asList("Trip hop", "Electronica"));

    artist.setComposedTracks(Arrays.asList(t1, t2, t3, t4, t5)); artist.setAlbums(Arrays.asList(a1, a2));

    assertEquals(0, validator.validate(t1).size() + validator.validate(t2).size() + validator.validate(t3).size() + validator.validate(t4).size());
    assertEquals(0, validator.validate(t5).size() + validator.validate(a1).size() + validator.validate(a2).size() + validator.validate(artist).size());

    datastore.save(Arrays.asList(t1, t2, t3, t4, t5));
    datastore.save(Arrays.asList(a1, a2));
    datastore.save(Arrays.asList(artist));
  }

  private void fillDbPearlJam()
  {
    Artist artist = new Artist(); artist.set_id(new ObjectId().toString()); artist.setName("Pearl Jam"); artist.setStartingYear(1990);

    Track t1 = new Track(); t1.set_id(new ObjectId().toString()); t1.setArtist_id(Arrays.asList(artist)); t1.setLength(5.43); t1.setName("Black"); 
    t1.setGenres(Arrays.asList("Alternative rock", "Power ballad")); t1.setPopularity(9.1); t1.setRatings(Arrays.asList(createRating(31.77, 3177)));

    Track t2 = new Track(); t2.set_id(new ObjectId().toString()); t2.setArtist_id(Arrays.asList(artist)); t2.setLength(4.53); t2.setName("Even flow");
    t2.setGenres(Arrays.asList("Alternative rock", "Grunge")); t2.setPopularity(5.3); t2.setRatings(Arrays.asList(createRating(9.56, 956)));

    Track t3 = new Track(); t3.set_id(new ObjectId().toString()); t3.setArtist_id(Arrays.asList(artist)); t3.setLength(5.18); t3.setName("Jeremy");
    t3.setGenres(Arrays.asList("Alternative rock", "Grunge")); t3.setPopularity(6.7); t3.setRatings(Arrays.asList(createRating(9.72, 972)));

    Album a1 = new Album(); a1.set_id(new ObjectId().toString()); a1.setFormats(Arrays.asList("LP", "Album", "Vinyl")); a1.setName("Ten");
    a1.setPopularity(9.8); a1.setReleaseYear(1991); a1.setTracks(Arrays.asList(t1, t2, t3)); a1.setAvailability("ES EN JP FR"); a1.setGenre("Grunge");
    a1.setReviews(Arrays.asList(
        createReview("David Fricke", "Very good", new Integer(4), Arrays.asList(createMedia("Rolling stone", "https://www.rollingstone.com/", "magazine"))),
        createReview("Steve Huey", "Excelent", "5/5", "AllMusic")));
    a1.setPrizes(Arrays.asList(
        createPrize("100 Greatest Guitar Albums of All Time", 15, 2006, null, null, Arrays.asList("Guitar world", "Acclaimed Music")),
        createPrize("The 100 Masterpieces", 68, 1993, null, null, Arrays.asList("Musik Express/Sounds", "Acclaimed Music")),
        createPrize("RIAA", 13000000, 2009, "Platinum disk", "United Kingdom", null)));

    Track t4 = new Track(); t4.set_id(new ObjectId().toString()); t4.setArtist_id(Arrays.asList(artist)); t4.setLength(4.19); t4.setName("No way");
    t4.setGenres(Arrays.asList("Alternative rock")); t4.setPopularity(4.2);

    Track t5 = new Track(); t5.set_id(new ObjectId().toString()); t5.setArtist_id(Arrays.asList(artist)); t5.setLength(3.49); t5.setName("Given to fly");
    t5.setGenres(Arrays.asList("Grunge")); t5.setPopularity(7.0);

    Track t6 = new Track(); t6.set_id(new ObjectId().toString()); t6.setArtist_id(Arrays.asList(artist)); t6.setLength(3.54); t6.setName("Do the evolution");
    t6.setGenres(Arrays.asList("Post-grunge", "Garage rock", "Punk rock")); t6.setPopularity(7.5);

    Album a2 = new Album(); a2.set_id(new ObjectId().toString()); a2.setFormats(Arrays.asList("LP", "Album", "Vinyl")); a2.setName("Yield"); a2.setPopularity(7.2);
    a2.setReleaseYear(1998); a2.setTracks(Arrays.asList(t4, t5, t6)); a2.setAvailability(Arrays.asList("ES", "EN", "FR", "PT", "JP"));
    a2.setGenres(Arrays.asList("Grunge", "Alternative rock"));

    artist.setComposedTracks(Arrays.asList(t1, t2, t3, t4, t5, t6)); artist.setLyricsTracks(Arrays.asList(t1, t2, t3, t6)); artist.setAlbums(Arrays.asList(a1, a2));

    assertEquals(0, validator.validate(t1).size() + validator.validate(t2).size() + validator.validate(t3).size() + validator.validate(t4).size());
    assertEquals(0, validator.validate(t5).size() + validator.validate(t6).size() + validator.validate(a1).size() + validator.validate(a2).size());
    assertEquals(0, validator.validate(artist).size());

    datastore.save(Arrays.asList(t1, t2, t3, t4, t5, t6));
    datastore.save(Arrays.asList(a1, a2));
    datastore.save(Arrays.asList(artist));
  }

  private Rating createRating(Double score, Integer voters)
  {
    Rating rating = new Rating();
    rating.setScore(score);
    rating.setVoters(voters);

    return rating;
  }

  private Prize createPrize(String event, Integer units, Integer year, String certification, String name, List<String> names)
  {
    Prize prize = new Prize();
    prize.setEvent(event);
    prize.setUnits(units);
    prize.setYear(year);
    prize.setCertification(certification);
    prize.setName(name);
    prize.setNames(names);

    return prize;
  }

  private Review createReview(String journalist, String rank, Object stars, Object media)
  {
    Review review = new Review();
    review.setJournalist(journalist);
    review.setRank(rank);
    review.setStars(stars);
    review.setMedia(media);

    return review;
  }

  private Media createMedia(String name, String url, String type)
  {
    Media media = new Media();
    media.setName(name);
    media.setUrl(url);
    media.setType(type);

    return media;
  }

  public static void main(String[] args)
  {
    MongoSongsDataManagement manager = new MongoSongsDataManagement("localhost", "mongosongs_running");
    manager.startDemo();
  }
}