package es.um.nosql.schemainference.test2;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.bson.types.ObjectId;
import org.mongodb.morphia.annotations.Property;
import javax.validation.constraints.NotNull;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.utils.IndexType;
import org.mongodb.morphia.annotations.IndexOptions;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;
import javax.validation.constraints.Pattern;


@Entity(value = "persons", noClassnameStored = true)
@Indexes({
  @Index(fields = {@Field(value = "age", type = IndexType.ASC)}),
  @Index(fields = {@Field(value = "name", type = IndexType.ASC), @Field(value = "surname", type = IndexType.ASC)}, options = @IndexOptions(sparse = true, background = false, unique = true)),
  @Index(fields = {@Field(value = "status", type = IndexType.TEXT, weight = 25)}, options = @IndexOptions(expireAfterSeconds = 330, name = "custom_index_name")),
  @Index(fields = {@Field(value = "isEmployed", type = IndexType.HASHED)})
})
public class Persons
{
  @Id
  @NotNull(message = "_id can't be null")
  private ObjectId _id;
  public ObjectId get_id() {return this._id;}
  public void set_id(ObjectId _id) {this._id = _id;}
  
  @Property
  @NotNull(message = "age can't be null")
  @Max(value = 100, message = "age must be > 18 && < 100")
  @Min(value = 18, message = "age must be > 18 && < 100")
  private Integer age;
  public Integer getAge() {return this.age;}
  public void setAge(Integer age) {this.age = age;}
  
  @Property
  @NotNull(message = "isEmployed can't be null")
  private Boolean isEmployed;
  public Boolean getIsEmployed() {return this.isEmployed;}
  public void setIsEmployed(Boolean isEmployed) {this.isEmployed = isEmployed;}
  
  @Property
  @NotNull(message = "isMarried can't be null")
  private Boolean isMarried;
  public Boolean getIsMarried() {return this.isMarried;}
  public void setIsMarried(Boolean isMarried) {this.isMarried = isMarried;}
  
  @Property
  @NotNull(message = "name can't be null")
  @Size(min = 3, max = 10, message = "name must have a length > 3 && < 10")
  private String name;
  public String getName() {return this.name;}
  public void setName(String name) {this.name = name;}
  
  @Property
  @NotNull(message = "status can't be null")
  @Pattern(regexp = "status[0-9]")
  private String status;
  public String getStatus() {return this.status;}
  public void setStatus(String status) {this.status = status;}
  
  @Property
  @NotNull(message = "surname can't be null")
  @Pattern(regexp = "surname1|surname2|surname3|surname4|surname5", flags = Pattern.Flag.CASE_INSENSITIVE)
  private String surname;
  public String getSurname() {return this.surname;}
  public void setSurname(String surname) {this.surname = surname;}
}
