wbt
===

"wannabe tempest"

a tempest (80's arcade game) clone in java, and a small project for playing with git.

playable, and fairly true to the original, though not a full feature-complete clone.

game summary:
the player (you) have a crawler, with which you must maneuver around the front of the current level and destroy the enemies coming forward towards you.  the enemies are:
  Exes: X-shaped enemies that fire missiles at you, and will chase you around the front of the board until they catch you, if not destroyed.
  Ex Pods: purple pods that come at you, which produce two exes when destroyed, or when they reach the front of the level.
  spikes: columns sometimes have spikes, which will destroy you if you hit them when clearing a level.  spikes also have spinners which fire missiles at you.

you also have a "superzapper", which is a special weapon that instantly destroys all exes (except for pods) that are on the board. 

control is via arrow keys and spacebar, and "S" to superzap.

##### to build, using ant build file:
    $ ant

##### to run:
    $ java -jar wbtempest.jar


###### Or without ant, ghetto it:
    $ cd src 
    $ javac wbtempest/*
    $ java wbtempest.WBTempest



