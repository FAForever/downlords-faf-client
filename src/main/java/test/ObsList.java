package test;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ObsList {

  public static void main(String[] args) {
    ObservableList<Object> arrayList = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());

    ObservableList<Object> unmodifiableObservableList = FXCollections.unmodifiableObservableList(arrayList);

    unmodifiableObservableList.addListener(new InvalidationListener() {
      @Override
      public void invalidated(Observable observable) {
        System.out.println("triggered");
      }
    });

    arrayList.add("A");
  }
}
