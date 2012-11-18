/*
 * Copyright (c) 2010-2012 by Ben de Waal. All Rights Reserved.
 *
 * This code is licensed under GPLv3. Other licenses are available directly from the author.
 *
 * No liability is assumed for whatever purpose, intended or unintended.
 */

package com.bronzecastle.iff.core.objects

import com.bronzecastle.iff.core.Relation
import com.bronzecastle.iff.core.model.Universe

/**
 * a player or an automated character
 */
trait IActor extends IThing {
  /**
   * direct action, like take bottle or go north
   */
  def act(action: IAction,ob: IObject) {
    action.act(this,ob)
  }

  /**
   * relational action, like put lamp on table
   */
  def act(action: IAction,ob1: IObject,rel: Relation,ob2: IObject) {
    action.act(this,ob1,rel,ob2)
  }

  /**
   * checks if the object is accessible to this actor
   */
  def canAccess(thing: IThing): Boolean = {
    // thing must be visible
    if (!canSee(thing)) return false
    // other crazy things, like isNotParalyzed
    //  ...
    // is accessible
    true
  }

  /**
   * checks that the object is visible to this actor
   */
  def canSee(thing: IThing): Boolean = {
    // actor and object must be in the same place
    if (getPlace.ID != thing.getPlace.ID) return false
    // actor can see place
    // isdark -> false
    // object is visible to place
    //  we start at the place and work down to determine if visible
    thing.listParents.reverse.foreach((p)=>{
      Universe().get[IPersistable](p.location) match {
        case place: IPlace => if (!place.listVisibleChildren.map(_.ID).contains(p.ID)) return false
        case thing: IThing => if (!thing.listVisibleChildren.map(_.ID).contains(p.ID)) return false
        case _ => {}
      }
    })
    // is visible
    true
  }
}
