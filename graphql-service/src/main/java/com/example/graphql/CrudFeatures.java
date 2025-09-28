
package com.example.graphql;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Feature toggles for CRUD operations on items.
 *
 * <p>Allows enabling or disabling create, read, update, and delete operations
 * at runtime via configuration (e.g., application.yml). Used by the GraphQL controller
 * to guard each operation.</p>
 */
@Component
@ConfigurationProperties(prefix = "features.crud")
public class CrudFeatures {
    /** Enable or disable create operation. */
    private boolean createEnabled = true;
    /** Enable or disable read operation. */
    private boolean readEnabled   = true;
    /** Enable or disable update operation. */
    private boolean updateEnabled = true;
    /** Enable or disable delete operation. */
    private boolean deleteEnabled = true;

    /** @return true if create is enabled */
    public boolean isCreateEnabled() { return createEnabled; }
    /** @param createEnabled set create enabled */
    public void setCreateEnabled(boolean createEnabled) { this.createEnabled = createEnabled; }
    /** @return true if read is enabled */
    public boolean isReadEnabled() { return readEnabled; }
    /** @param readEnabled set read enabled */
    public void setReadEnabled(boolean readEnabled) { this.readEnabled = readEnabled; }
    /** @return true if update is enabled */
    public boolean isUpdateEnabled() { return updateEnabled; }
    /** @param updateEnabled set update enabled */
    public void setUpdateEnabled(boolean updateEnabled) { this.updateEnabled = updateEnabled; }
    /** @return true if delete is enabled */
    public boolean isDeleteEnabled() { return deleteEnabled; }
    /** @param deleteEnabled set delete enabled */
    public void setDeleteEnabled(boolean deleteEnabled) { this.deleteEnabled = deleteEnabled; }
}
