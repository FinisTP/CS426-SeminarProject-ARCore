package com.example.secondar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.graphics.ColorSpace;
import android.graphics.Point;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Bundle;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.ar.core.Anchor;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.collision.Ray;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.rendering.Texture;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private Scene scene;
    private Camera camera;
    private ModelRenderable bulletRenderable;
    private boolean timeStarted = false;
    private int placedObjects = 0;
    private TextView counter;
    private Point point;
    private SoundPool soundPool;
    private int sound;
    private Button fireButton;



    private ArFragment arFragment;
    private ArrayList<Integer> imagesPath = new ArrayList<Integer>();
    private ArrayList<String> namesPath = new ArrayList<>();
    private ArrayList<String> modelNames = new ArrayList<>();
    AnchorNode anchorNode;
    private Button btnRemove;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Display display = getWindowManager().getDefaultDisplay();
        point = new Point();
        display.getRealSize(point);

        arFragment = (MyArFragment)getSupportFragmentManager().findFragmentById(R.id.fragment);
        scene = arFragment.getArSceneView().getScene();
        camera = scene.getCamera();

        setEnemyToTheScene();
        builtBullet();
        counter = findViewById(R.id.counter);
        loadSoundPool();

        fireButton = findViewById(R.id.fire);
        fireButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                shoot();
            }
        });

        btnRemove = (Button)findViewById(R.id.remove);
        getImages();

        arFragment.setOnTapArPlaneListener((hitResult, plane, motionEvent) -> {

            Anchor anchor = hitResult.createAnchor();

            ModelRenderable.builder()
                .setSource(this,Uri.parse(Common.model))
                .build()
                .thenAccept(modelRenderable -> addModelToScene(anchor,modelRenderable));

        });


        btnRemove.setOnClickListener(view -> removeAnchorNode(anchorNode));
    }

    private void getImages() {

        imagesPath.add(R.drawable.table);
        imagesPath.add(R.drawable.bookshelf);
        imagesPath.add(R.drawable.lamp);
        imagesPath.add(R.drawable.odltv);
        imagesPath.add(R.drawable.clothdryer);
        imagesPath.add(R.drawable.chair);
        namesPath.add("Table");
        namesPath.add("BookShelf");
        namesPath.add("Lamp");
        namesPath.add("Old Tv");
        namesPath.add("Cloth Dryer");
        namesPath.add("Chair");
        modelNames.add("table.sfb");
        modelNames.add("model.sfb");
        modelNames.add("lamp.sfb");
        modelNames.add("tv.sfb");
        modelNames.add("cloth.sfb");
        modelNames.add("chair.sfb");

        initiateRecyclerView();
    }

    private void initiateRecyclerView() {

        LinearLayoutManager layoutManager = new LinearLayoutManager(this,LinearLayoutManager.HORIZONTAL,false);
        RecyclerView recyclerView = (RecyclerView)findViewById(R.id.recyclerview);
        recyclerView.setLayoutManager(layoutManager);
        RecyclerviewAdapter adapter = new RecyclerviewAdapter(this,namesPath,imagesPath,modelNames);
        recyclerView.setAdapter(adapter);

    }

    private void addModelToScene(Anchor anchor, ModelRenderable modelRenderable) {

        anchorNode = new AnchorNode(anchor);
        TransformableNode node = new TransformableNode(arFragment.getTransformationSystem());
        node.setParent(anchorNode);
        node.setRenderable(modelRenderable);
        arFragment.getArSceneView().getScene().addChild(anchorNode);
        node.select();
    }

    public void removeAnchorNode(AnchorNode nodeToremove) {
        if (nodeToremove != null) {
            arFragment.getArSceneView().getScene().removeChild(nodeToremove);
            nodeToremove.getAnchor().detach();
            nodeToremove.setParent(null);
            nodeToremove = null;
        }
    }

    private void loadSoundPool() {
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_GAME)
                .build();
        soundPool = new SoundPool.Builder().setMaxStreams(1).setAudioAttributes(attributes).build();
        sound = soundPool.load(this, R.raw.blop_sound, 1);
    }

    private void shoot() {
        Ray ray = camera.screenPointToRay(point.x / 2f, point.y / 2f);
        Node node = new Node();
        node.setRenderable(bulletRenderable);
        scene.addChild(node);

        new Thread(() -> {
            for (int i = 0; i < 100; ++i) {
                int finalI = i;
                runOnUiThread(() -> {
                    Vector3 v = ray.getPoint(finalI * .2f);
                    node.setWorldPosition(v);

                    Node nodeContact = scene.overlapTest(node);
                    if (nodeContact != null) {
                        placedObjects++;
                        counter.setText("Hit objects: " + placedObjects);
                        scene.removeChild(nodeContact);
                        scene.removeChild(node);


                        soundPool.play(sound,1,1,1,0,1);

                    }
                });

                try {
                    Thread.sleep(20);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            runOnUiThread(() -> {
                scene.removeChild(node);
            });

        }).start();
    }

    private void setEnemyToTheScene() {
        ModelRenderable.builder()
                .setSource(this, Uri.parse("ball.sfb"))
                .build()
                .thenAccept(modelRenderable -> {
                    for (int i = 0; i < 20; ++i) {
                        Node node = new Node();
                        node.setRenderable(modelRenderable);
                        Random random = new Random();
                        float x = random.nextInt(8) - 4f;
                        float y = random.nextInt(4) - 2;
                        float z = random.nextInt(10) - 5;

                        Vector3 position = new Vector3(x,y, -z);
                        node.setWorldPosition(position);
                        node.setLocalRotation(Quaternion.axisAngle(new Vector3(0,1,0), random.nextInt(230)));
                        scene.addChild(node);

                    }
                });

    }

    private void builtBullet() {
        Texture.builder()
                .setSource(this, R.drawable.ball)
                .build()
                .thenAccept(texture -> {
                    MaterialFactory.makeOpaqueWithTexture(this,texture).thenAccept(
                            material -> {
                                bulletRenderable = ShapeFactory.makeSphere(0.02f, new Vector3(0f,0f,0f), material);
                            }
                    );
                });
    }



}
