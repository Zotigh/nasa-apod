package edu.cnm.deepdive.nasaapod.model.entity;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import java.util.Calendar;

@Entity(
    foreignKeys = @ForeignKey(
        entity = Apod.class,
        parentColumns = "apod_id", childColumns = "apod_id",
        onDelete = ForeignKey.CASCADE
    ),
    indices = @Index(value = {"apod_id", "timestamp"})
)
public class Access {

  @ColumnInfo(name = "access_id")
  @PrimaryKey(autoGenerate = true)
  private long id;

  @ColumnInfo(name = "apod_id", index = true)
  private long apodId;

  private Calendar timestamp = Calendar.getInstance();

  /**
   * Returns the autogenerated primary key of this instance.
   *
   * @return primary key value.
   */
  public long getId() {
    return id;
  }

  /**
   * Sets the primary key of this instance. This method is invoked by Room to set the autogenerated
   * value of a new instance, and when loading an existing instance from the database.
   *
   * @param id primary key value.
   */
  public void setId(long id) {
    this.id = id;
  }


  /**
   * Returns the foreign key value referencing the primary key of the {@link Apod} entity table.
   *
   * @return foreign key value, referencing primary key of {@link Apod}.
   */
  public long getApodId() {
    return apodId;
  }

  /**
   * Sets the foreign key referencing the primary key of the {@link Apod} entity table. This method
   * is invoked by app code to associate this instance with an {@link Apod} instance, and by Room
   * when loading an existing instance from the database.
   *
   * @param apodId foreign key value, referencing primary key of {@link Apod}.
   */
  public void setApodId(long apodId) {
    this.apodId = apodId;
  }

  /**
   * Returns the date-time timestamp set on creation of this instance.
   *
   * @return creation date-time timestamp.
   */
  public Calendar getTimestamp() {
    return timestamp;
  }

  /**
   * Sets the creation date-time timestamp of this instance. This method is invoked by Room when
   * loading the instance from the database.
   *
   * @param timestamp creation date-time timestamp.
   */
  public void setTimestamp(Calendar timestamp) {
    this.timestamp = timestamp;
  }

}
