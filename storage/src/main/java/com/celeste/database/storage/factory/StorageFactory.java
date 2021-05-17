package com.celeste.database.storage.factory;

import com.celeste.database.shared.exception.database.FailedConnectionException;
import com.celeste.database.storage.model.database.StorageType;
import com.celeste.database.storage.model.database.provider.Storage;
import java.lang.reflect.Constructor;
import java.util.Properties;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;

/**
 * The StorageFactory creates the connection between the application and the server. It has support
 * to MySQL, MongoDB, SQLite, PostgreSQL and H2.
 *
 * <p>To establish connection with the Storage, you should always provide
 * the Driver and Credentials in the Properties and the ConnectionType.</p>
 *
 * <p>The type of the connection are LOCAL (The Storage is installed in
 * the machine the program will execute or in another machine that doesn't block remote access) or
 * CLUSTER, a connection created by a cluster in another machine that is only used for the
 * Storage.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StorageFactory {

  private static final StorageFactory INSTANCE = new StorageFactory();

  /**
   * Creates a new instance of the provider provided in the field driver of the properties.
   *
   * @param properties     Properties with Driver and Credentials
   * @return Messenger
   * @throws FailedConnectionException Throws if the connection failed
   */
  public Storage start(@NotNull final Properties properties) throws FailedConnectionException {
    try {
      final String driver = properties.getProperty("driver");
      final StorageType storage = StorageType.getStorage(driver);

      return storage.getProvider().getConstructor(Properties.class).newInstance(properties);
    } catch (Throwable throwable) {
      throw new FailedConnectionException(throwable);
    }
  }

  /**
   * Returns the instance of the StorageFactory
   *
   * @return StorageFactory
   */
  public static StorageFactory getInstance() {
    return INSTANCE;
  }

}