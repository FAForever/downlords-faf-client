package com.faforever.client.leaderboard;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public class MockLeaderboardService implements LeaderboardService {


  @Override
  public List<LadderInfoBean> getLadderInfo() {
    return new List<LadderInfoBean>() {
      @Override
      public int size() {
        return 0;
      }

      @Override
      public boolean isEmpty() {
        return false;
      }

      @Override
      public boolean contains(Object o) {
        return false;
      }

      @Override
      public Iterator<LadderInfoBean> iterator() {
        return null;
      }

      @Override
      public Object[] toArray() {
        return new Object[0];
      }

      @Override
      public <T> T[] toArray(T[] a) {
        return null;
      }

      @Override
      public boolean add(LadderInfoBean ladderInfoBean) {
        return false;
      }

      @Override
      public boolean remove(Object o) {
        return false;
      }

      @Override
      public boolean containsAll(Collection<?> c) {
        return false;
      }

      @Override
      public boolean addAll(Collection<? extends LadderInfoBean> c) {
        return false;
      }

      @Override
      public boolean addAll(int index, Collection<? extends LadderInfoBean> c) {
        return false;
      }

      @Override
      public boolean removeAll(Collection<?> c) {
        return false;
      }

      @Override
      public boolean retainAll(Collection<?> c) {
        return false;
      }

      @Override
      public void clear() {

      }

      @Override
      public boolean equals(Object o) {
        return false;
      }

      @Override
      public int hashCode() {
        return 0;
      }

      @Override
      public LadderInfoBean get(int index) {
        return null;
      }

      @Override
      public LadderInfoBean set(int index, LadderInfoBean element) {
        return null;
      }

      @Override
      public void add(int index, LadderInfoBean element) {

      }

      @Override
      public LadderInfoBean remove(int index) {
        return null;
      }

      @Override
      public int indexOf(Object o) {
        return 0;
      }

      @Override
      public int lastIndexOf(Object o) {
        return 0;
      }

      @Override
      public ListIterator<LadderInfoBean> listIterator() {
        return null;
      }

      @Override
      public ListIterator<LadderInfoBean> listIterator(int index) {
        return null;
      }

      @Override
      public List<LadderInfoBean> subList(int fromIndex, int toIndex) {
        return null;
      }
    }
    ;

/*
    private StringProperty username;
    private IntegerProperty rank;
    private IntegerProperty rating;
    private IntegerProperty gamesPlayed;
    private FloatProperty score;
    private FloatProperty winLossRatio;
    private StringProperty division*/

}}
