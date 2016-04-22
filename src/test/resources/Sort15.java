import java.util.*;
import javax.management.relation.*;

public class Sort15 {

  public static void main(String[] a){
    // add our roles to the RoleList
    RoleList libraryList = new RoleList();


    populateRoleList(libraryList);


    for( Object each : libraryList){
      final Role eachRole = (Role) each;

      System.out.println(eachRole);
    }
  }

  private static void populateRoleList(RoleList libraryList){
    // building the owner Role
    List<String> ownerList = new ArrayList<String>();
    ownerList.add("Peter");  // can only add owner to an owner role cardinality defined as 1
    final Role ownerRole = new Role("owner", ownerList);

    // building the book role
    List<String> bookList = new ArrayList<String>();
    // we can have between 1 and 4 books more than 4 invalidates out relation and less than 1 invalidates it
    bookList.add("Book1");
    bookList.add("Book2");
    bookList.add("Book3");
    Role bookRole = new Role("books", bookList);

    libraryList.add(ownerRole);
    libraryList.add(bookRole);
  }
}