package com.victor;

import java.util.Vector;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.Shape;
import com.badlogic.gdx.physics.box2d.World;

public class DebrisWorld implements ContactListener {
	World world;
	Vector<Body> debrisList;
	Body player;
	Body door;
	Box2DDebugRenderer debug = new Box2DDebugRenderer();
	int countContact;
	int countDebris;
	float timeDrop;
	float timeLeft;

	public enum State {
		WIN,
		DEAD,
		DROPPING,
		STOPPED
	}

	State state = State.DROPPING;

	public void reset() {
		debrisList = new Vector<Body>();
		countContact = 0;
		countDebris = 0;
		timeDrop = 0f;
		timeLeft = DebrisParam.TIME_LIMIT;

		Vector2 gravity = new Vector2(0.0f, DebrisParam.GRAVITY);
		world = new World(gravity, true);
		world.setContactListener(this);

		createFloor();
		createLeftWall();
		createRightWall();
		createPlayer();
	}

	public void tick(float delta) {
		if (state == State.WIN || state == State.DEAD)
			return;

		timeDrop += delta;
		timeLeft -= delta;

		if (timeLeft <= 0f) {
			state = State.DEAD;
			return;
		}

		if (timeDrop >= DebrisParam.DROP_INTERVAL
				&& countDebris < DebrisParam.MAX_DEBRIS) {
			timeDrop = 0f;
			countDebris ++;
			Body body = createDebrisRandom();
			debrisList.add(body);
			/*
			if (countFreeze >= 0) {
				world.getDebrisList().get(countFreeze).setType(BodyDef.BodyType.StaticBody);
			}
			countFreeze += 1;
			*/
		}

		if (countDebris == DebrisParam.MAX_DEBRIS
				&& state != State.STOPPED) {
			state = State.STOPPED;
			createDoor();
		}

		int adjustIter = 1 + (int) (delta / (1000f/DebrisParam.FPS));
		float dt = (delta/1000f) / adjustIter;
		int i;
		for(i = 0; i < adjustIter; ++i) {
			world.step(dt, 10, 10);
		}
	}

	public Vector<Body> getDebrisList() {
		return debrisList;
	}

	public void createPlayer() {
		player = createDebrisCircle(0, DebrisParam.STAGE_HEIGHT/5f, DebrisParam.PLAYER_SIZE);
	}

	public void movePlayer(Vector2 force) {
		//player.applyForce(force, player.getWorldCenter());
		player.applyLinearImpulse(force, player.getWorldCenter(), true);
	}

	public Body getPlayer() {
		return player;
	}

	public Body getDoor() {
		return door;
	}

	public boolean isPlayerJumpable() {
		return countContact > 0;
	}

	public Body createDebrisRandom() {
		// randomize size
		int dice = (int)(Math.random() * DebrisParam.DEBRIS_PROB_MAX);
		int i;
		for (i = 0; i < DebrisParam.DEBRIS_PROB.length; ++i) {
			dice -= DebrisParam.DEBRIS_PROB[i];
			if (dice <= 0)
				break;
		}
		float size = DebrisParam.DEBRIS_SIZE[i];

		// randomize drop position
		float x = -DebrisParam.STAGE_WIDTH / 2f
			+ (float) (Math.random() * (DebrisParam.STAGE_WIDTH - DebrisParam.WALL_WIDTH));

		// randomize shape
		/*
		int type = (int) (Math.random() * 3);
		Body body = null;
		if (type == 0) {
			body = createDebrisBox(x, DebrisParam.STAGE_HEIGHT, size, size, 0, 0, 0);
		} else if (type == 1) {
			body = createDebrisTriangle(x, DebrisParam.STAGE_HEIGHT, size);
		} else if (type == 2) {
			body = createDebrisCircle(x, DebrisParam.STAGE_HEIGHT, size);
		} else {
			// should not reach here
		}
		*/

		Body body = createDebrisBox(x, DebrisParam.STAGE_HEIGHT*1.5f, size, size, 0, 0, 0);
		body.setUserData(size);
		return body;
	}

	public void createDoor() {
		door = createDebrisBox(0,
				DebrisParam.STAGE_HEIGHT*2f,
				DebrisParam.DOOR_WIDTH,
				DebrisParam.DOOR_HEIGHT,
				0, 0, 0);
	}

	public Body createDebrisBox(float x, float y, float w, float h, float cx, float cy,
			float angle) {
		PolygonShape shape = new PolygonShape();
		shape.setAsBox((float) w, (float) h,
				new Vector2((float) cx, (float) cy), angle);

		return createDebris(x, y, shape);
	}

	public Body createDebrisTriangle(float x, float y, float e) {
		PolygonShape shape = new PolygonShape();
		Vector2[] vlist = { new Vector2(0f, 0f),
				new Vector2(e * 0.5f, -e * 0.866f), new Vector2((float) e, 0f) };
		shape.set(vlist);

		return createDebris(x, y, shape);
	}

	public Body createDebrisCircle(float x, float y, float radius) {
		CircleShape shape = new CircleShape();
		shape.setRadius(radius);

		return createDebris(x, y, shape);
	}

	public Body createDebris(float x, float y, Shape shape) {
		BodyDef bd = new BodyDef();
		bd.position.set((float) x, (float) y);

		FixtureDef fd = new FixtureDef();
		fd.shape = shape;
		fd.density = 1.0f;
		fd.friction = 0.3f;
		fd.restitution = 0f;

		Body body = world.createBody(bd);
		body.createFixture(fd);
		body.setType(BodyDef.BodyType.DynamicBody);

		return body;
	}

	public void createFloor() {
		PolygonShape wallshape = new PolygonShape();
		wallshape.setAsBox(
				DebrisParam.STAGE_WIDTH/2f,
				DebrisParam.WALL_WIDTH,
				new Vector2(0f, DebrisParam.WALL_WIDTH), 0f);

		FixtureDef fd = new FixtureDef();
		fd.shape = wallshape;
		fd.density = 1.0f;

		BodyDef bd = new BodyDef();
		bd.position.set(0f, 0f);

		Body wall = world.createBody(bd);
		wall.createFixture(fd);
		wall.setType(BodyDef.BodyType.StaticBody);
	}

	public void createLeftWall() {
		PolygonShape wallshape = new PolygonShape();
		wallshape.setAsBox(
				DebrisParam.WALL_WIDTH,
				DebrisParam.WALL_HEIGHT,
				new Vector2(-DebrisParam.WALL_WIDTH, DebrisParam.WALL_HEIGHT), 0f);

		FixtureDef fd = new FixtureDef();
		fd.shape = wallshape;
		fd.density = 1.0f;

		BodyDef bd = new BodyDef();
		bd.position.set(-DebrisParam.STAGE_WIDTH/2f, 0f);

		Body wall = world.createBody(bd);
		wall.createFixture(fd);
		wall.setType(BodyDef.BodyType.StaticBody);
	}

	public void createRightWall() {
		PolygonShape wallshape = new PolygonShape();
		wallshape.setAsBox(
				DebrisParam.WALL_WIDTH,
				DebrisParam.WALL_HEIGHT,
				new Vector2(DebrisParam.WALL_WIDTH, DebrisParam.WALL_HEIGHT), 0f);

		FixtureDef fd = new FixtureDef();
		fd.shape = wallshape;
		fd.density = 1.0f;

		BodyDef bd = new BodyDef();
		bd.position.set(DebrisParam.STAGE_WIDTH/2f, 0f);

		Body wall = world.createBody(bd);
		wall.createFixture(fd);
		wall.setType(BodyDef.BodyType.StaticBody);
	}

	public State getState() {
		return state;
	}

	public float getTimeLeft() {
		return timeLeft;
	}

	@Override
	public void beginContact(Contact contact) {
		// if the player hits something
		if (player == contact.getFixtureA().getBody()
				|| player == contact.getFixtureB().getBody()) {
			countContact += 1;
			// if the player hits the door
			if (door != null &&
					(door == contact.getFixtureA().getBody()
							|| door == contact.getFixtureB().getBody())) {
				state = State.WIN;
			}
		}
	}

	@Override
	public void endContact(Contact contact) {
		// if the player left something
		if (player == contact.getFixtureA().getBody()
				|| player == contact.getFixtureB().getBody()) {
			countContact -= 1;
		}
	}

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {

    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {

    }
}
