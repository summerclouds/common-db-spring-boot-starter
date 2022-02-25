package org.summerclouds.common.db.xdb1;

import java.util.UUID;

import org.summerclouds.common.core.error.MException;
import org.summerclouds.common.db.DbComfortableObject;
import org.summerclouds.common.db.DbManager;
import org.summerclouds.common.db.annotations.DbEntity;
import org.summerclouds.common.db.annotations.DbIndex;
import org.summerclouds.common.db.annotations.DbPersistent;
import org.summerclouds.common.db.annotations.DbPrimaryKey;
import org.summerclouds.common.db.annotations.DbRelation;
import org.summerclouds.common.db.model.Person;
import org.summerclouds.common.db.relation.RelSingle;
import org.summerclouds.common.db.sql.DbConnection;

@DbEntity
public class Book extends DbComfortableObject {

    private UUID id;
    private String name;
    private UUID[] authorId;
    private int pages;
    private UUID lendToId;
    private RelSingle<Person> lendTo = new RelSingle<Person>();

    @DbPrimaryKey
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    @DbPersistent
    @DbIndex("1")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @DbPersistent
    public UUID[] getAuthorId() {
        return authorId;
    }

    public void setAuthorId(UUID[] author) {
        this.authorId = author;
    }

    @DbPersistent
    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    @DbPersistent
    public UUID getLendToId() {
        return lendToId;
    }

    public void setLendToId(UUID lendTo) {
        this.lendToId = lendTo;
    }

    public Person getLendToPerson() throws MException {
        if (lendToId == null) return null;
        return ((DbManager) getDbHandler()).getObject(Person.class, getLendToId());
    }

    @Override
    public void doPreCreate(DbConnection con) {
        // TODO Auto-generated method stub

    }

    @Override
    public void doPreSave(DbConnection con) {
        // TODO Auto-generated method stub

    }

    @Override
    public void doPreDelete(DbConnection con) {
        // TODO Auto-generated method stub

    }

    @Override
    public void doPostLoad(DbConnection con) {
        // TODO Auto-generated method stub

    }

    @Override
    public void doPostDelete(DbConnection con) {
        // TODO Auto-generated method stub

    }

    @DbRelation(target = Person.class)
    public RelSingle<Person> getLendTo() {
        return lendTo;
    }

    @Override
    public boolean isAdbPersistent() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void doPostCreate(DbConnection con) {
        // TODO Auto-generated method stub

    }
    
}
