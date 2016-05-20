public class AvoidComments {

  /**
   * Example of a Person class with unnecessary JavaDoc on methods.
   */
  class BadPerson {

    private String name;

    /**
     * Returns the first name.
     *
     * @return a string containing the first name
     */
    public String getName() {
      return name;
    }
  }

  /**
   * Example of a Person class that doesn't need JavaDoc on methods.
   */
  class GoodPerson {

    private String firstName;

    // There's no reason to comment that "getFirstName" returns the first name.
    // There's also no reason to comment that it returns a string containing the first name.
    public String getFirstName() {
      return firstName;
    }
  }

  /**
   * Example of a superfluous comment.
   */
  private void badComment() {
    // Time in milliseconds before exploding
    long time = 5000;
  }

  /**
   * Example of how comments can be avoided.
   */
  private void avoidedComment() {
    long millisBeforeExploding = 5000;
  }
}
