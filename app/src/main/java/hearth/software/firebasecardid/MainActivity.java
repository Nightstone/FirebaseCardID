package hearth.software.firebasecardid;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    ImageView cardIDImageView;

    //Variable pour la prise de photo et la sauvegarde en
    //variable de la photo en taille réelle
    String currentPhotoPath;
    static final int REQUEST_TAKE_PHOTO = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cardIDImageView = findViewById(R.id.cardIDImageView);
    }

    //ACTIONS DES BOUTONS
    //Une action de bouton doit être définie de la sorte
    //public void nomDeLaFonction(View view)
    //Puis on doit ajouter l'attribut onClick sur le bouton
    //Dans l'activity.xml

    //Fonction pour prendre la photo, afficher la thumbnail dans
    //l'imageView, et stocker en variable la photo en taille réelle
    public void takePhoto(View takePhotoButton) {

        //On crée l'objet Intent qui contiendra notre activity de
        //Camera
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        //On regarde si on a bien configuré notre application
        //pour utiliser la caméra
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

            //On crée un fichier grâce à la fonction createImageFile()
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.d("JLE", "Le fichier n'a pas été crée");
            }

            //Si le fichier a bien été crée, on récupère alors l'Uri
            //de l'image après avoir configurer un fileProvider
            //Pour créer le fileProvider :
            //File -> New -> Android Resource File
            //Name : file_paths
            //Resource type : XML
            //Root element : paths
            //Ne pas oublier de configurer le provider dans le AndroidManifest !
            //Pour plus d'infos : https://developer.android.com/reference/androidx/core/content/FileProvider.html
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "firebase.cardid.project",
                        photoFile);
                //On met ensuite cette Uri dans notre Intent pour que
                //l'activity de Camera puisse sauvegarder notre image
                //en taille réelle à cet endroit
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

                //On lance l'activity en attendant un résultat (la photo) de la
                //part de l'activity de Camera. On récupère le résultat dans
                //la fonction onActivityResult plus bas.
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    //Fonction pour traiter la photo taille réelle avec le MLKit de
    //Firebase.
    public void getTextFromPhoto(View getTextFromPhotoButton) {
        //On récupère l'image réelle
        if (currentPhotoPath != null) {
            Bitmap fullSizePhoto = BitmapFactory.decodeFile(currentPhotoPath);

            //On transforme notre image taille réelle en FirebaseVisionImage
            FirebaseVisionImage firebaseImage = FirebaseVisionImage.fromBitmap(fullSizePhoto);

            //On instancie ensuite l'objet Firebase pour trouver le texte sur
            //une image
            //On peut choisir entre la version locale ou via le cloud.
            //La version locale est gratuite, la version cloud est payante (voir sur la console Firebase).
            //Pour la version locale, il ne faut pas oublier de configurer le Android Manifest
            FirebaseVisionTextRecognizer textRecognizer = FirebaseVision.getInstance().getOnDeviceTextRecognizer();

            //On lance la reconnaissance de texte dans l'image
            textRecognizer.processImage(firebaseImage)
                    .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                        @Override
                        public void onSuccess(FirebaseVisionText firebaseVisionText) {
                            //Ici la reconnaissance a bien été effectué
                            //On a alors un objet de type firebaseVisionText
                            //Pour voir comment est composé un firebaseVisionText
                            //Voici la doc : https://firebase.google.com/docs/reference/android/com/google/firebase/ml/vision/text/FirebaseVisionText
                            //Ici on affiche tout le texte trouvé
                            Log.d("JLE", "Voici le texte contenu : " + firebaseVisionText.getText());
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //Ici ça s'est mal passé
                            Log.d("JLE", "Erreur lors de la reconnaissance de texte");
                        }
                    });
        } else {
            Toast.makeText(this, "Vous devez prendre une photo avant", Toast.LENGTH_SHORT).show();
        }
    }

    //Fonction pour enregistrer une donnée sur Firebase
    public void saveData(View saveDataButton) {
        //Une base de données Firebase de type Realtime Database est défini
        //par un objet JSON. Une référence contient des ensembles de clés/valeurs.

        //On récupère l'instance de la BDD Firebase
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();

        //On récupère ensuite une référence
        DatabaseReference reference = firebaseDatabase.getReference("foo");

        //Pour ajouter une valeur, on utilise setValue().
        //Cependant cela ne rajoute pas une valeur dans la référence mais à la référence elle même.
        //Pour rajouter une valeur à la suite dans la référence, il faudra rajouter push() devant setValue()

        reference.setValue("Coucou");
        reference.push().setValue("Coucou2");

        //Vous pouvez aussi définir un objet JAVA en paramètre : Les ensembles clés/valeurs vont être
        //crées en consequence !

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            //On récupère la photo et on l'affiche avec les ratios corrects
            setResizePictureInImageView();
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void setResizePictureInImageView() {
        // Get the dimensions of the View
        int targetW = cardIDImageView.getWidth();
        int targetH = cardIDImageView.getHeight();

        // Get the dimensions of the bitmap
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        bmOptions.inPurgeable = true;

        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions);
        cardIDImageView.setImageBitmap(bitmap);
    }
}
