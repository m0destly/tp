package hirehive.address.model.person;

import java.util.function.Predicate;

import hirehive.address.commons.util.ToStringBuilder;
import hirehive.address.logic.Messages;
import hirehive.address.model.tag.Tag;

/**
 * Tests that a {@code Person}'s {@code Tag}s matches the given Tag.
 */
public class PersonContainsTagPredicate implements PersonPredicate {
    private final Tag tag;
    public PersonContainsTagPredicate(Tag tag) {
        this.tag = tag;
    }

    @Override
    public boolean test(Person person) {
        return person.getTag().equals(tag);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof PersonContainsTagPredicate)) {
            return false;
        }

        PersonContainsTagPredicate otherPersonContainsTagPredicate = (PersonContainsTagPredicate) other;
        return this.tag.equals(otherPersonContainsTagPredicate.tag);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("tag", this.tag).toString();
    }

    @Override
    public String getSuccessString() {
        return String.format(Messages.MESSAGE_FILTER_OVERVIEW_TAG, tag);
    }
}
