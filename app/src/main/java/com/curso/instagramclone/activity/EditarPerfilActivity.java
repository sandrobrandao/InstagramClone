package com.curso.instagramclone.activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.curso.instagramclone.R;
import com.curso.instagramclone.helper.ConfiguracaoFirebase;
import com.curso.instagramclone.helper.Permissao;
import com.curso.instagramclone.helper.UsuarioFirebase;
import com.curso.instagramclone.model.Usuario;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditarPerfilActivity extends AppCompatActivity {
    private CircleImageView imagePerfil;
    private TextView textAlterarFoto;
    private TextInputEditText editNomePerfil;
    private TextInputEditText editEmailPerfil;
    private Button buttonSalvarAlteracoes;
    private Usuario usuarioLogado;
    private static final int SELECAO_GALERIA = 200;
    private StorageReference storageRef;
    private String identificadorUsuario;
    private String[] permissoesNecessarias = new String[] {
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_editar_perfil );

        // validar permissoes
        Permissao.validarPermissoes( permissoesNecessarias,this,1 );

        // configuracoes iniciais
        usuarioLogado = UsuarioFirebase.getDadosUsuarioLogado();
        storageRef = ConfiguracaoFirebase.getFirebaseStorage();
        identificadorUsuario = UsuarioFirebase.getIdentificadorUsuario();

        // configura a toolbar
        Toolbar toolbar = findViewById( R.id.toolbarPrincipal );
        toolbar.setTitle( "Editar perfil" );
        setSupportActionBar( toolbar );

        getSupportActionBar().setDisplayHomeAsUpEnabled( true );
        getSupportActionBar().setHomeAsUpIndicator( R.drawable.ic_close_black_24dp );

        inicializarComponentes();

        // recupera dados do usuario
        FirebaseUser usuarioPerfil = UsuarioFirebase.getUsuarioAtual();
        editNomePerfil.setText( usuarioPerfil.getDisplayName().toUpperCase() );
        editEmailPerfil.setText( usuarioPerfil.getEmail() );
        Uri url = usuarioPerfil.getPhotoUrl();
        if( url != null ) {
            Glide.with(EditarPerfilActivity.this )
                    .load( url )
                    .into( imagePerfil );
        } else {
            imagePerfil.setImageResource( R.drawable.avatar );
        }

        // atualizar nome do usuario
        buttonSalvarAlteracoes.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View v ) {
                String nomeAtualizado = editNomePerfil.getText().toString();

                // atualiza o nome do perfil
                UsuarioFirebase.atualizarNomeUsuario( nomeAtualizado );

                // atualiza o nome no banco de dados
                usuarioLogado.setNome( nomeAtualizado );
                usuarioLogado.atualizar();
                Toast.makeText(EditarPerfilActivity.this,
                        "Dados alterados com sucesso!",
                        Toast.LENGTH_SHORT ).show();
            }
        });

        // altera a foto de perfil do usuario
        textAlterarFoto.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View v ) {
                Intent i = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                if( i.resolveActivity(getPackageManager()) != null ){
                    startActivityForResult(i, SELECAO_GALERIA );
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if ( resultCode == RESULT_OK ){
            Bitmap imagem = null;
            try {
                //Selecao apenas da galeria
                switch ( requestCode ){
                    case SELECAO_GALERIA:
                        Uri localImagemSelecionada = data.getData();
                        imagem = MediaStore.Images.Media.getBitmap(getContentResolver(), localImagemSelecionada );
                        break;
                }

                //Caso tenha sido escolhido uma imagem
                if ( imagem != null ){
                    //Configura imagem na tela
                    imagePerfil.setImageBitmap( imagem );

                    //Recuperar dados da imagem para o firebase
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    imagem.compress( Bitmap.CompressFormat.JPEG, 70, baos );
                    byte[] dadosImagem = baos.toByteArray();

                    //Salvar imagem no firebase
                    StorageReference imagemRef = storageRef
                            .child( "imagens" )
                            .child( "perfil" )
                            .child( identificadorUsuario + ".jpeg" );
                    UploadTask uploadTask = imagemRef.putBytes( dadosImagem );
                    uploadTask.addOnFailureListener( new OnFailureListener() {
                        @Override
                        public void onFailure( @NonNull Exception e ) {
                            Toast.makeText(EditarPerfilActivity.this,
                                    "Erro ao fazer upload da imagem",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }).addOnSuccessListener( new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess( UploadTask.TaskSnapshot taskSnapshot ) {
                            // recupera local da foto
                            Uri url = taskSnapshot.getDownloadUrl();
                            atualizarFotoUsuario( url );

                            Toast.makeText(EditarPerfilActivity.this,
                                    "Sucesso ao fazer upload da imagem",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }

    public void atualizarFotoUsuario( Uri url ) {
        // atualiza foto no perfil
        UsuarioFirebase.atualizarFotoUsuario( url );

        // atualiza foto no firebase
        usuarioLogado.setCaminhoFoto( url.toString() );
        usuarioLogado.atualizar();
        Toast.makeText(EditarPerfilActivity.this,
                "Sua foto foi atualizada!",
                Toast.LENGTH_SHORT).show();
    }
    public void inicializarComponentes() {
        imagePerfil = findViewById( R.id.imagePerfil);
        textAlterarFoto = findViewById( R.id.textAlterarFoto);
        editNomePerfil = findViewById( R.id.editNomePerfil );
        editEmailPerfil = findViewById( R.id.editEmailPerfil );
        buttonSalvarAlteracoes = findViewById( R.id.buttonSalvarAlteracoes );
        editEmailPerfil.setFocusable( false );
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return false;
    }
}