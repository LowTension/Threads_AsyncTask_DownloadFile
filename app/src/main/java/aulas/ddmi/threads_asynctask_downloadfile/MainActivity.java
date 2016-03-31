package aulas.ddmi.threads_asynctask_downloadfile;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Esta classe faz o download de um arquivo.
 * Esta versão não controla se ele já existe ou não no sistema de arquivos do dispositivo. Simplesmente baixa o arquivo.
 * @author Vagner Pinto da Silva 
 */
public class MainActivity extends AppCompatActivity {

    // Progress Dialog
    private ProgressDialog mProgressDialog;

    // url do arquvo para download
    private static String file_url = "http://livroandroid.com.br/livro/carros/classicos/ford_mustang.mp4";

    //TAG para o LogCat
    private final String TAG = "Threads";

    //AsyncTask
    DownloadTask downloadTask;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mProgressDialog = new ProgressDialog(MainActivity.this);
        mProgressDialog.setMessage("Executando o download. Aguarde ...");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(true);

        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                MainActivity.this.downloadTask.cancel(true);
            }
        });

    }

    //trata o evento onClick do Button
    public void iniciarDownload(View v) {
        downloadTask = new DownloadTask();
        downloadTask.execute(file_url);
    }

    //classe interna de AsyncTask
    private class DownloadTask extends AsyncTask<String, Integer, String> { //<Params,Progress,Result>

        @Override
        protected void onPreExecute() { //antes de iniciar a execução da Thread
            super.onPreExecute();
            mProgressDialog.show();
        }

        @Override
        protected String doInBackground(String... params) { //executa a Thread
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;

            try {

                //inicializa a conexão com o servidor
                URL url = new URL(params[0]); //recupera a URL que está em params
                connection = (HttpURLConnection) url.openConnection(); //usa esta url para abrir uma conexão com o servidor
                connection.connect(); //se conecta ao servidor

                // espera a resposta HTTP 200 OK, senão uma mensagem de erro do servidor
                // se não for OK, retorna uma menssagem para ser tratada em onPostExecute()
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    Log.d(TAG, "O servidor retornou erro. Mensagem HTTP " + connection.getResponseCode() + " " + connection.getResponseMessage());
                    return "O servidor retornou erro. Mensagem HTTP " + connection.getResponseCode() + " " + connection.getResponseMessage();
                }

                long fileLength = connection.getContentLength(); //obtém o tamanho do arquivo, se isto estiver manifestado no servidor
                Log.i(TAG, "tamanho total arquivo: " + fileLength);
                //se o servidor não forneceu o tamanho do arquivo, então varre um inputStream para saber seu tamanho
                if(fileLength < 1){
                    // download do arquivo em forma de fluxo de bytes
                    InputStream input_clone = new BufferedInputStream(url.openStream(), 8192);
                    byte data[] = new byte[1024]; //um array de bytes
                    long total = 0; //para armazenar o tamanho do arquivo
                    int count = 0;
                    while ((count = input_clone.read(data)) != -1) { //enquanto não chegar ao final do input
                        total += count;
                        Log.d(TAG, "Lendo o tamanho do arquivo: " + total);
                    }
                    fileLength = total; //armazena o tamanho do arquivo
                    input_clone.close(); //libera o recurso
                }


                // download do arquivo em forma de fluxo de bytes
                input = connection.getInputStream();
                // local para onde o fluxo será direcionado. Neste caso, o arquivo "ford_mustang.mp4"
                output = new FileOutputStream(Environment.getExternalStorageDirectory().toString() + "/Download/ford_mustang.mp4");

                byte data[] = new byte[4096];
                long total = 0;
                int count = 0;
                while ((count = input.read(data)) != -1) {
                    // se o usuário cancelar o download com o back button
                    if (isCancelled()) {
                        input.close();
                        return "Download cancelado.";
                    }
                    total += count;
                    Log.d(TAG, "Lendo o tamanho do arquivo: " + total);
                    // publica o progresso do download. O médoto publishProgress chama o método onProgressUpdate()
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                Log.d(TAG, "Erro ao realizar o download " + e.toString());
                return "Erro ao realizar o download.";
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }

            return "Download realizado.";

        }

        @Override
        protected void onProgressUpdate(Integer... values) { //atualiza o ProgressBar
            // atualiza o porcentual do progressbar
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(100);
            mProgressDialog.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            // destrói o progressDialog. Faz com que ele desapareça da UI
            mProgressDialog.dismiss();
            Toast.makeText(MainActivity.this,result, Toast.LENGTH_LONG).show();
        }
    }//fim classe interna
}//fim classe externa
