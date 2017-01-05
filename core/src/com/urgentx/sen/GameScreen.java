package com.urgentx.sen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.DragListener;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.StretchViewport;

import java.util.ArrayList;
import java.util.Iterator;


/**
 * Screen with main game loop, logic, asset loading.
 */

public class GameScreen implements Screen {

    final Sen game;

    //Initialise environment objects and game variables.

    //Libgdx / Scene2D variables.
    private OrthographicCamera camera;
    public SpriteBatch batch;
    public BitmapFont font;
    private Texture  trainerImage, keeperImage, attackerImage, baseImage, attackerSheet, attackerSheet2, buyMenuBackground;
    TextureRegion[] attackerAnimationFrames, attacker2AnimationFrames;
    Animation animation, strongerAnimation; //anims for attacker types
    TextureAtlas textureAtlas; //for registering drawables and regions, add it to our skin
    private Label baseHpLabel, scoreLabel, cashLabel, buyLabel, eradLabel, slowLabel;
    private boolean keepersDefending;
    private int keeperSpawn; //increment with each tick(), i.e. should a keeper spawn yet
    Stage stage;
    private Skin skin, windowSkin;

    //Sound variables.
    private Sound dropSound;
    private Music music;

    //User input variables.
    Vector3 touchPos = new Vector3();
    private Rectangle buyRect, eradRect, slowRect;
    float closestDistance;

    //Lists of game objects.
    private ArrayList<Keeper> keepers; //could use LGX List
    private ArrayList<Attacker> attackers;
    private ArrayList<Label> labels;

    //Test objects.
    private Trainer trainer1;
    private Trainer trainer2;
    private Keeper keeper1;
    private Attacker attacker1;

    //Variables for player use.
    private int baseHp = 500;
    private int score, cash;
    private int numEradicates, numSlows; //Number of powerups in player inventory.

    //Variables for attackers.
    private static final int DEFAULT_ATTACKER_SPEED = 5;
    private int attackerSpeedFactor;
    private int attackerSpawn;

    //Variables for timekeeping
    private long lastTick;
    float timer; //use this to determine how much time has passed since last tick in render() method.
    private boolean firstTick; //set this to false after we call an initial tick().

    //Tracker of game state. Change this when necessary, render() highly dependant on this.
    public enum State{
        PAUSE, RUN, RESUME, STOPPED
    }

    private State state = State.RUN; //initial State.

    //Random test/unused variables.
    private Polygon bucket; //better to use Polygon than rect (rotation easy)
    ShapeRenderer shapeRenderer; //to render our polygons

    public GameScreen(final Sen gam){
        this.game = gam;

        //load images
        trainerImage = new Texture(Gdx.files.internal("trainer.png"));
        keeperImage = new Texture(Gdx.files.internal("keeper.png"));
        attackerImage = new Texture(Gdx.files.internal("attacker.png"));
        baseImage = new Texture(Gdx.files.internal("base.png"));
        attackerSheet = new Texture(Gdx.files.internal("attackersheet.png"));
        attackerSheet2 = new Texture(Gdx.files.internal("attackersheet2.png"));
        buyMenuBackground = new Texture(Gdx.files.internal("buymenubg.png"));

        TextureRegion [] [] tmpFrames = TextureRegion.split(attackerSheet, 25, 25);
        TextureRegion [] [] tmpFrames1 = TextureRegion.split(attackerSheet2, 50, 50);
        //initialize animation
        attackerAnimationFrames = new TextureRegion[4];
        attacker2AnimationFrames = new TextureRegion[4];

        int index = 0;
        for(int i = 0 ; i < 2; i++){
            for(int j = 0; j < 2; j++){
                attackerAnimationFrames[index] = tmpFrames[j][i];
                attacker2AnimationFrames[index++] = tmpFrames1[j][i];
            }
        }

        animation = new Animation(1f/4f, attackerAnimationFrames);
        strongerAnimation = new Animation(1f/4f, attacker2AnimationFrames);

        //load sounds
        dropSound = Gdx.audio.newSound(Gdx.files.internal("drop.mp3"));
        music = Gdx.audio.newMusic(Gdx.files.internal("music.mp3"));

        //start bg music
        music.setLooping(true);
        music.play();


        ////////////////////////////////////////////////////

        trainer1 = new Trainer(600);
        trainer2 = new Trainer(700);

        keepers = new ArrayList<Keeper>();
        attackers = new ArrayList<Attacker>();

        keeperSpawn = 0;

        numEradicates = 0;
        numSlows = 0;


        stage = new Stage(new StretchViewport(800, 480)); //scene2d handles camera perspective
        Gdx.input.setInputProcessor(stage);

        textureAtlas = new TextureAtlas();
        textureAtlas.addRegion("default-window", buyMenuBackground, 5, 5, 50, 50); //set up window background

        skin = new Skin(Gdx.files.internal("gfx/uiskin.json"), textureAtlas); //initialise skin with JSON file and our image-loaded TA
        skin.add("default", new Label.LabelStyle(new BitmapFont(), Color.BLUE));
        skin.add("trainer", trainerImage);
        skin.add("keeper", keeperImage);
        skin.add("attacker", attackerImage);
        skin.add("base", baseImage);

        //create actors
        Image validTargetImage = new Image(skin, "trainer");
        validTargetImage.setBounds(700, 80, 10, 300);
        stage.addActor(validTargetImage);

        Image baseActorImage = new Image (skin, "base");
        baseActorImage.setBounds(500, 100, 50, 200);
        stage.addActor(baseActorImage);

        Image invalidTargetImage = new Image(skin, "trainer");
        invalidTargetImage.setBounds(600, 80, 10, 300);
        stage.addActor(invalidTargetImage);

        baseHpLabel = new Label("HP " + baseHp, skin);
        baseHpLabel.setBounds(500, 200, 40, 30);
        stage.addActor(baseHpLabel);

        scoreLabel = new Label("Score " + score, skin);
        scoreLabel.setBounds(400, 450, 40, 30);
        stage.addActor(scoreLabel);

        cashLabel = new Label("$$ " + cash, skin);
        cashLabel.setBounds(400, 430, 40, 30);
        stage.addActor(cashLabel);

        eradRect = new Rectangle(560, 430, 40, 30);
        eradLabel = new Label("Erads " + numEradicates, skin);
        eradLabel.setBounds(560, 430, 40, 30);
        stage.addActor(eradLabel);
        eradLabel.addListener(new InputListener() {
            public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
                eradicate();
                return false;
            }

            public void touchUp (InputEvent event, float x, float y, int pointer, int button) {

            }
        });

        slowRect = new Rectangle(620, 430, 40, 30);
        slowLabel = new Label("Slows " + numSlows, skin);
        slowLabel.setBounds(620, 430, 40, 30);
        stage.addActor(slowLabel);
        slowLabel.addListener(new InputListener() {
            public boolean touchDown (InputEvent event, float x, float y, int pointer, int button) {
                slow();
                return false;
            }

            public void touchUp (InputEvent event, float x, float y, int pointer, int button) {

            }
        });

        buyRect = new Rectangle(700, 450, 40 , 30);
        buyLabel = new Label("BUY", skin);
        buyLabel.setBounds(700, 450, 40, 30);
        stage.addActor(buyLabel);

        closestDistance = 1000; //default distance to closest defender for attackers


        //Timekeeping variables
        timer = 0f;
        firstTick = true;


        score = 0;
        cash = 0;

        attackerSpeedFactor = DEFAULT_ATTACKER_SPEED;



    }

    @Override
    public void render(float delta){

        //Check if our render method is called for the first time this execution.
        if(firstTick){
            tick(); //If so, call an initial tick().
            firstTick = false; //No more ticks from here.
        }

        switch(state){
            case RUN:
                //First, we check if we need to tick (i.e. 10 seconds has passed)
                timer += delta; //Add time since last frame to our timer float at each render()
                if(timer >= 10){ //If these deltas add up to 10 seconds, we call tick()...
                    tick();
                    timer -= 10; // and set the timer back 10 seconds to start the loop again.
                }

                //Poll for input
                if (Gdx.input.isTouched()){
                    stage.getCamera().unproject(touchPos.set(Gdx.input.getX(0),Gdx.input.getY(0), 0 ));   //cam fixed for controls
                    if(buyRect.contains(touchPos.x, touchPos.y)){
                        openBuyMenu();
                  }
                }

                keepersDefending = false;

                Iterator<Keeper> keeperIterator = keepers.iterator();
                while(keeperIterator.hasNext()) {       //iterate through all Keepers
                    Keeper keeper = keeperIterator.next();

                    keeper.update();
                    keeper.getSourceImage().setX(keeper.getX());    //update Actor location
                    keeper.getSourceImage().setY(keeper.getY());

                    keeper.getLabel().setX(keeper.getX() + 20);
                    keeper.getLabel().setY(keeper.getY() + 18);
                    keeper.getLabel().setText(Integer.toString(keeper.getLevel()));

                    if(keeper.getHp() <= 0){        //check for death
                        keeper.getSourceImage().remove();
                        keeper.getLabel().remove();
                        keeper = null;
                        keeperIterator.remove();
                    } else {

                        if (keeper.getX() < 500) {      //check if Keeper in defending zone
                            keepersDefending = true;
                        }
                    }
                }


                Iterator<Attacker>  attackerIterator = attackers.iterator();
                while(attackerIterator.hasNext()){      //iterate through all Attackers

                    Attacker attacker = attackerIterator.next();

                    attacker.update();
                    attacker.getSourceImage().setX(attacker.getX());        //update Actor location
                    attacker.getSourceImage().setY(attacker.getY());



                    if(attacker.getHp() <= 0){      //check for death
                        attackerIterator.remove();
                        killAttacker(attacker);

                    } else {
                        if (keepersDefending) {
                            if (! keepers.isEmpty()) {       //check for collisions

                                Keeper closest = null;
                                closestDistance = 10000000; //reset so that all keepers are considered once more


                                Iterator<Keeper> keeperIterator2 = keepers.iterator();
                                while (keeperIterator2.hasNext()) {   //traverse keepers
                                    Keeper keeper = keeperIterator2.next();
                                    float keeperX = keeper.getX();
                                    float keeperY = keeper.getY();

                                    float xd = Math.abs(keeperX - attacker.getX()); //X distance from keeper to attacker
                                    float yd = Math.abs(keeperY - attacker.getY()); //Y distance from keeper to attacker
                                    float distance = (float) Math.sqrt(xd * xd + yd * yd);
                                    Gdx.app.log("mytag1", "distance = " + distance + "keeperX = " + keeper.getX());
                                    if (distance < closestDistance) {
                                        closest = keeper;
                                        closestDistance = distance;
                                    }
                                }

                                if(closest != null) {

                                    //Gdx.app.log("mytag1", " --" + closest.getX());
                                    //move attacker toward keeper
                                    float xSpeed = (closest.getX() - attacker.getX()) / 500;
                                    float ySpeed = (closest.getY() - attacker.getY()) / 500;
                                    //float factor = 0.3f / (float) Math.sqrt(xSpeed * xSpeed + ySpeed * ySpeed);

                                    attacker.setSpeedX(xSpeed * attackerSpeedFactor);
                                    attacker.setSpeedY(ySpeed * attackerSpeedFactor);

                                    //check for collision, deal damage
                                    if (Math.abs(attacker.getX() - closest.getX()) < 10 && Math.abs(attacker.getY() - closest.getY()) < 10) {
                                        closest.damage(attacker.getAttackDamage());
                                        attacker.damage(closest.getAttackDamage());

                                    }
                                }


                            }
                        } else {        //else move attacker toward base

                            float attackerX = attacker.getX(); //move these to global variables
                            float attackerY = attacker.getY();
                            float keeperX;
                            float keeperY;
                            float xd, yd;  //x distance, y distance from keeper
                            float distance;

                            float xSpeed = (525 - attacker.getX()) / 500;
                            float ySpeed = (200 - attacker.getY()) / 500;


                            attacker.setSpeedX(xSpeed);
                            attacker.setSpeedY(ySpeed);

                            if (attackerX > 480) {      //damage base
                                baseHp -= 1;
                            }

                        }
                    }

                }


                trainer1.update();
                trainer2.update();

                baseHpLabel.setText("HP " + baseHp);
                scoreLabel.setText("Score " + score);
                cashLabel.setText("$$ " + cash);
                eradLabel.setText("Erads" + numEradicates);
                slowLabel.setText("Slows " + numSlows);

                // if (TimeUtils.nanoTime() - lastTick > 1000000000 * 100) tick();


                //keeperSourceImage1.setX(keeper1.getX());
                // keeperSourceImage1.setY(keeper1.getY());

                Gdx.gl.glClearColor(1f, 0.9f, 1f, 1);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                stage.act(Gdx.graphics.getDeltaTime());
                stage.draw();

                break;
            case PAUSE:
                Gdx.gl.glClearColor(1f, 0.9f, 1f, 1);
                Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
                stage.draw();
                break;
            case RESUME:
                state = State.RUN;
                break;
            case STOPPED:
                break;
        }


    }

    private void killAttacker(Attacker attacker){
        score += attacker.getBounty();
        cash += attacker.getBounty();
        attacker.getSourceImage().remove();
        attackers.remove(attacker);
        attacker = null;
    }

    private void slow() {
        if(numSlows >= 1) { //Check if player has any slows left.
            attackerSpeedFactor = 1; // slow down attackers.
            Timer t = new Timer(); //Set up a timer to resume normal speed.
            t.scheduleTask(new Timer.Task() {
                @Override
                public void run() {
                    attackerSpeedFactor = DEFAULT_ATTACKER_SPEED;
                }
            }, 3);
            t.start();
            numSlows--; //Take away a charge.
        }

    }

    private void eradicate() {
        if(numEradicates >= 1) { //Check if player has any erads left.
            Iterator<Attacker> attackerIterator = attackers.iterator(); //Kill all attackers
            while (attackerIterator.hasNext()) {
                Attacker attacker = attackerIterator.next();
                killAttacker(attacker);
            }
            numEradicates--; //Remove a charge.
        }

    }

    private void openBuyMenu() {

        pause();

        //independent pause menu
        final Window pause = new Window("BUY", skin);
        pause.padTop(64);

        //build TextButtons with listeners
        TextButton continueButton = new TextButton("continue", skin);
        continueButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                pause.remove(); //could also setVisible(false)
                state = State.RESUME;
            }
        });

        TextButton eradButton = new TextButton("Buy an Eradication", skin);
        eradButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                numEradicates++;
            }
        });

        TextButton slowButton = new TextButton("Buy a Slow", skin);
        slowButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                numSlows++;
            }
        });

        //add buttons to Window, add Window to stage
        pause.add(continueButton).row();
        pause.add(eradButton).row();
        pause.add(slowButton).row();
        pause.setSize(stage.getWidth() / 1.5f, stage.getHeight() / 1.5f);
        pause.setPosition(stage.getWidth() / 2 - pause.getWidth() / 2, stage.getWidth() / 2 - pause.getWidth() / 2);
        //pause.pack(); //packs window around contents
        stage.addActor(pause);

    }

    @Override
    public void dispose() {
        batch.dispose();
        dropSound.dispose();
        music.dispose();
        trainerImage.dispose();
        keeperImage.dispose();
        attackerImage.dispose();
        baseImage.dispose();
        attackerSheet.dispose();
        attackerSheet2.dispose();
        buyMenuBackground.dispose();
        textureAtlas.dispose();
        skin.dispose();
        windowSkin.dispose();
        stage.dispose();
        font.dispose();
    }

    public void tick() {
        Gdx.app.log("ks", "ks: " + keeperSpawn % 3);
        if(keeperSpawn % 3 == 0 && keepers.size() < 2) {
            spawnKeeper();
        }

        int spawnChance = MathUtils.random(100);
        Gdx.app.log("ks", "spawnchance: " + spawnChance);

        if(spawnChance < 50){
            spawnAttacker();
        } else {
            spawnStrongerAttacker();
        }
        lastTick = TimeUtils.nanoTime();
        keeperSpawn++;

        score++;
    }

    public void spawnKeeper() {

        //create Scene2D Actor for each Keeper
        final Image sourceImage = new Image(skin, "keeper");
        int yPos = MathUtils.random(50,400);
        int xPos = MathUtils.random(400,500);
        sourceImage.setBounds(xPos, yPos, 50, 50);
        stage.addActor(sourceImage);

        Label label = new Label("1", skin);

        stage.addActor(label);

        final Keeper keeper = new Keeper(xPos, yPos, 1, sourceImage, label);

        label.setX(keeper.getX() + 20);
        label.setY(keeper.getY() + 18);

        //set up Listener for each Keeper
        sourceImage.addListener(new DragListener() {

            //follow touch when dragged
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                //keeperSourceImage1.moveBy(x-keeperSourceImage1.getWidth()/2,y-keeperSourceImage1.getHeight()/2);
                keeper.increaseX(x - sourceImage.getWidth() / 2);
                keeper.increaseY(y - sourceImage.getHeight() / 2);

            }

            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                return true;
            }

            //add/remove Keeper to/from Trainer
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                if (event.getStageX() > 700 && event.getStageX() < 740) {
                    trainer2.clearKeeper();
                    trainer2.setKeeper(keeper);
                } else if (event.getStageX() > 600 && event.getStageX() < 640) {
                    trainer1.clearKeeper();
                    trainer1.setKeeper(keeper);
                } else {
                    trainer1.clearKeeper(keeper);
                    trainer2.clearKeeper(keeper);
                }
            }
        });

        keepers.add(keeper);
    }

    public void spawnAttacker() {

        //create Scene2D Actor for each Attacker, no need for Listeners.
        float yPos = MathUtils.random(50,350); //randomise y position

        final AnimatedImage sourceImage = new AnimatedImage(animation);
        sourceImage.setBounds(10,yPos,25,25);

        stage.addActor(sourceImage);

        final Attacker attacker = new Attacker(10, yPos, sourceImage, 1);
        attackers.add(attacker);
        attackerSpawn++;
    }

    public void spawnStrongerAttacker(){
        //create Scene2D Actor for each Attacker, no need for Listeners.
        float yPos = MathUtils.random(50,350); //randomise y position

        final AnimatedImage strongerSourceImage = new AnimatedImage(strongerAnimation);
        strongerSourceImage.setBounds(10,yPos,50,50);
        stage.addActor(strongerSourceImage);

        final Attacker attacker = new Attacker(10, yPos, strongerSourceImage, 2);
        attackers.add(attacker);
        attackerSpawn++;
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void show() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void pause() {
        this.state = State.PAUSE;
    }

    @Override
    public void resume() {
        this.state = State.RESUME;
    }

}
