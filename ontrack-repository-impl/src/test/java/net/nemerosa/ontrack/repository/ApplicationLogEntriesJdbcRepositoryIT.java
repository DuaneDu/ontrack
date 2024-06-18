package net.nemerosa.ontrack.repository;

import net.nemerosa.ontrack.model.structure.NameDescription;
import net.nemerosa.ontrack.model.support.ApplicationLogEntry;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class ApplicationLogEntriesJdbcRepositoryIT extends AbstractRepositoryJUnit4TestSupport {

    @Autowired
    private ApplicationLogEntriesRepository repository;

    @Test
    public void no_detail() {
        repository.log(
                ApplicationLogEntry.error(
                        new NullPointerException(),
                        NameDescription.nd("MyType", "My description"),
                        "Some information"
                )
        );
    }

    @Test
    public void details() {
        repository.log(
                ApplicationLogEntry.error(
                        new NullPointerException(),
                        NameDescription.nd("MyType", "My description"),
                        "Some information"
                )
                        .withDetail("detail1", "value1")
                        .withDetail("detail2", "value2")
        );
    }

    @Test
    public void authentication() {
        repository.log(
                ApplicationLogEntry.error(
                        new NullPointerException(),
                        NameDescription.nd("MyType", "My description"),
                        "Some information"
                )
                        .withAuthentication("user")
        );
    }

}
