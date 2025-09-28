package com.example.graphql;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "features.crud")
public class CrudFeatures {
  private boolean createEnabled = true;
  private boolean readEnabled   = true;
  private boolean updateEnabled = true;
  private boolean deleteEnabled = true;

  public boolean isCreateEnabled() { return createEnabled; }
  public void setCreateEnabled(boolean createEnabled) { this.createEnabled = createEnabled; }
  public boolean isReadEnabled() { return readEnabled; }
  public void setReadEnabled(boolean readEnabled) { this.readEnabled = readEnabled; }
  public boolean isUpdateEnabled() { return updateEnabled; }
  public void setUpdateEnabled(boolean updateEnabled) { this.updateEnabled = updateEnabled; }
  public boolean isDeleteEnabled() { return deleteEnabled; }
  public void setDeleteEnabled(boolean deleteEnabled) { this.deleteEnabled = deleteEnabled; }
}
