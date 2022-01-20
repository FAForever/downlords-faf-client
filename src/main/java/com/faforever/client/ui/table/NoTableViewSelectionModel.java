package com.faforever.client.ui.table;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumnBase;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TableView.TableViewSelectionModel;

@SuppressWarnings("rawtypes")
public class NoTableViewSelectionModel<T> extends TableViewSelectionModel<T> {

  private final ObservableList<TablePosition> noSelectedCells = FXCollections.emptyObservableList();

  public NoTableViewSelectionModel(TableView<T> tableView) {
    super(tableView);
  }

  @Override
  public ObservableList<TablePosition> getSelectedCells() {
    return noSelectedCells;
  }

  @Override
  public boolean isSelected(int row, TableColumn<T, ?> column) {
    return false;
  }

  @Override
  public void select(int row, TableColumn<T, ?> column) {

  }

  @Override
  public void clearAndSelect(int row, TableColumn<T, ?> column) {

  }

  @Override
  public void clearSelection(int row, TableColumn<T, ?> column) {

  }

  @Override
  public void selectLeftCell() {

  }

  @Override
  public void selectRightCell() {

  }

  @Override
  public void selectAboveCell() {

  }

  @Override
  public void selectBelowCell() {

  }

  @Override
  public boolean isSelected(int row, TableColumnBase<T, ?> column) {
    return false;
  }

  @Override
  public boolean isSelected(int index) {
    return false;
  }
}
