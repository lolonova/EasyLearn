package fr.eduko.app

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Purple = Color(0xFF7357E8)
private val Ink = Color(0xFF20264A)
private val Lilac = Color(0xFFEDE9FF)
private val Mint = Color(0xFFDDF8EF)
private val Bg = Color(0xFFF7F6FC)

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  val prefs = getSharedPreferences("eduko_profile", MODE_PRIVATE)
  setContent {
   MaterialTheme(colorScheme = lightColorScheme(primary = Purple, background = Bg)) {
    EdukoApp(prefs.getString("name","") ?: "", prefs.getString("level","4e") ?: "4e") { n,l,needs,pet ->
     prefs.edit().putString("name",n).putString("level",l).putString("needs",needs.joinToString(",")).putString("pet",pet).apply()
    }
   }
  }
 }
}

private enum class Page { PROFILE, HOME, LESSON, QUIZ, TEACHER }

@Composable
private fun EdukoApp(savedName:String, savedLevel:String, save:(String,String,List<String>,String)->Unit) {
 var name by remember { mutableStateOf(savedName) }
 var level by remember { mutableStateOf(savedLevel) }
 var pet by remember { mutableStateOf("🐉") }
 var needs by remember { mutableStateOf(setOf<String>()) }
 var page by remember { mutableStateOf(if(savedName.isBlank()) Page.PROFILE else Page.HOME) }
 var request by remember { mutableStateOf("") }
 var lesson by remember { mutableStateOf("Pythagore") }
 var question by remember { mutableStateOf("") }
 var qIndex by remember { mutableIntStateOf(0) }
 var score by remember { mutableIntStateOf(0) }
 var done by remember { mutableStateOf(false) }
 var photoReady by remember { mutableStateOf(false) }
 val voice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
  val words=result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
  if(!words.isNullOrBlank()){ request=words; lesson=if(words.contains("Pythagore",true)) "Pythagore" else words; page=Page.LESSON }
 }
 val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
  photoReady=bitmap!=null
  if(bitmap!=null) request="Photo de cours ajoutée — analyse à connecter"
 }
 when(page) {
  Page.PROFILE -> Profile(name,{name=it},level,{level=it},pet,{pet=it},needs,{needs=it}) {
   save(name,level,needs.toList(),pet); page=Page.HOME
  }
  Page.HOME -> Home(name.ifBlank{"Aventurier"},pet,request,{request=it},
   { val i=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_LANGUAGE,"fr-FR")
    putExtra(RecognizerIntent.EXTRA_PROMPT,"Que souhaites-tu apprendre ?")
   }; runCatching { voice.launch(i) } },
   {camera.launch(null)},
   {lesson=if(request.contains("Pythagore",true)) "Pythagore" else request.ifBlank{"Pythagore"};page=Page.LESSON},
   {page=Page.PROFILE},{page=Page.TEACHER},photoReady)
  Page.LESSON -> Lesson(lesson,pet,{page=Page.HOME},{qIndex=0;score=0;done=false;page=Page.QUIZ},{page=Page.TEACHER})
  Page.QUIZ -> Quiz(qIndex,score,done,{ok->if(ok)score++;if(qIndex==4)done=true else qIndex++},{page=Page.LESSON},{qIndex=0;score=0;done=false})
  Page.TEACHER -> Teacher(question,{question=it},pet,{page=Page.LESSON})
 }
}

@Composable private fun Profile(name:String,onName:(String)->Unit,level:String,onLevel:(String)->Unit,pet:String,onPet:(String)->Unit,needs:Set<String>,onNeeds:(Set<String>)->Unit,go:()->Unit) {
 Screen {
  Heading("Bienvenue dans Eduko","Ton espace pour apprendre à ta façon")
  Panel(Lilac) {
   Text("Comment t'appelles-tu ?",fontWeight=FontWeight.Bold,color=Ink)
   Entry(name,onName,"Ton prénom")
   Text("Ta classe",fontWeight=FontWeight.Bold,color=Ink)
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ listOf("5e","4e","3e").forEach{ FilterChip(level==it,{onLevel(it)},{Text(it)}) } }
  }
  Panel(Color.White) {
   Text("Choisis ton compagnon",fontSize=19.sp,fontWeight=FontWeight.Bold,color=Ink)
   Text("Tu pourras en changer quand tu voudras.",color=Color.Gray,fontSize=12.sp)
   Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
    listOf("🐉","🦊","🐼","🤖","🐱").forEach { p ->
     Box(Modifier.size(52.dp).background(if(pet==p)Lilac else Bg,CircleShape).clickable{onPet(p)},contentAlignment=Alignment.Center){Text(p,fontSize=29.sp)}
    }
   }
  }
  Panel(Mint) {
   Text("Mon profil d'apprentissage",fontWeight=FontWeight.Bold,color=Ink,fontSize=18.sp)
   Text("Facultatif : sélectionne les adaptations qui peuvent t'aider. Eduko ne pose aucun diagnostic.",fontSize=12.sp,color=Color.DarkGray)
   listOf("Dyslexie","Dysorthographie","Dyscalculie","Dyspraxie","Dysphasie","TDAH","TSA","Concentration","Mémorisation","Autre besoin").forEach { item ->
    Row(verticalAlignment=Alignment.CenterVertically) {
     Checkbox(item in needs,{checked->onNeeds(if(checked)needs+item else needs-item)})
     Text(item,color=Ink)
    }
   }
  }
  Button(go,Modifier.fillMaxWidth().height(54.dp),enabled=name.isNotBlank(),shape=RoundedCornerShape(17.dp)){Text("C'est parti ! ✨",fontSize=17.sp,fontWeight=FontWeight.Bold)}
 }
}

@Composable private fun Home(name:String,pet:String,request:String,onRequest:(String)->Unit,voice:()->Unit,photo:()->Unit,go:()->Unit,profile:()->Unit,teacher:()->Unit,photoReady:Boolean) {
 Screen {
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
   Column { Text("EDUKO",color=Purple,fontWeight=FontWeight.Black,letterSpacing=3.sp);Text("Salut "+name+" !",color=Ink,fontSize=27.sp,fontWeight=FontWeight.ExtraBold) }
   Text("⚙️",Modifier.clickable{profile()},fontSize=24.sp)
  }
  Panel(Brush.linearGradient(listOf(Color(0xFF7760E8),Color(0xFF9B82F4)))) {
   Row(verticalAlignment=Alignment.CenterVertically) {
    Box(Modifier.size(88.dp).background(Color.White.copy(alpha=.2f),CircleShape),contentAlignment=Alignment.Center){Text(pet,fontSize=55.sp)}
    Column(Modifier.weight(1f)) {
     Text("TON COMPAGNON D'ÉTUDE",color=Color.White.copy(alpha=.8f),fontSize=11.sp)
     Text("On apprend ensemble ?",color=Color.White,fontSize=20.sp,fontWeight=FontWeight.Bold)
     Text("Je suis là pour t'aider, à ton rythme.",color=Color.White,fontSize=12.sp)
    }
   }
   Text("Que veux-tu apprendre aujourd'hui ?",color=Color.White,fontWeight=FontWeight.Bold)
   Entry(request,onRequest,"Ex. : Je veux réviser Pythagore…")
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
    Button(voice,Modifier.weight(1f),colors=ButtonDefaults.buttonColors(containerColor=Color.White,contentColor=Purple)){Icon(Icons.Default.Mic,null);Text("Parler")}
    Button(go,Modifier.weight(1f),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFFFFD88A),contentColor=Ink)){Text("Réviser")}
   }
  }
  Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
   Tile("📸","Photographier mon cours",Modifier.weight(1f),photo)
   Tile("🎓","Parler au professeur",Modifier.weight(1f),teacher)
  }
  if(photoReady) Panel(Mint){Text("Photo prise ! L'analyse intelligente sera ajoutée dans une prochaine version.",color=Ink)}
  Text("Mes matières",color=Ink,fontSize=20.sp,fontWeight=FontWeight.Bold)
  listOf("📐  Mathématiques","📚  Français","🇬🇧  Anglais","🇮🇹  Italien","🌍  Histoire-Géographie","🧪  Physique-Chimie","🌱  SVT").chunked(2).forEach { row ->
   Row(horizontalArrangement=Arrangement.spacedBy(9.dp)) {
    row.forEach { subject -> Panel(Color.White,Modifier.weight(1f)){Text(subject,color=Ink,fontWeight=FontWeight.SemiBold,modifier=Modifier.clickable{onRequest("Réviser "+subject.substringAfter("  "));go()})} }
    if(row.size==1) Spacer(Modifier.weight(1f))
   }
  }
 }
}

@Composable private fun Lesson(title:String,pet:String,back:()->Unit,quiz:()->Unit,teacher:()->Unit) {
 Screen {
  Row(verticalAlignment=Alignment.CenterVertically){IconButton(back){Icon(Icons.Default.ArrowBack,null)};Column{Text("TA LEÇON",color=Purple,fontWeight=FontWeight.Bold,fontSize=12.sp);Text(title,color=Ink,fontSize=25.sp,fontWeight=FontWeight.ExtraBold)}}
  Panel(Lilac){Text(pet+"  Mission du jour",fontSize=19.sp,fontWeight=FontWeight.Bold,color=Ink);Text("On avance étape par étape. Tu peux prendre ton temps !",color=Ink)}
  Panel(Color.White) {
   Text("📖  Le cours",color=Ink,fontSize=20.sp,fontWeight=FontWeight.Bold)
   Text("Le théorème de Pythagore s'applique dans un triangle rectangle. Le carré de la longueur de l'hypoténuse est égal à la somme des carrés des deux autres côtés.",color=Ink)
   Text("a² + b² = c²",color=Purple,fontWeight=FontWeight.ExtraBold,fontSize=25.sp)
   Text("Exemple : si les deux côtés de l'angle droit mesurent 3 cm et 4 cm, l'hypoténuse mesure 5 cm.",color=Ink)
   Text("À retenir : l'hypoténuse est le côté opposé à l'angle droit. C'est le plus long côté.",color=Ink)
  }
  Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Tile("🎧","Écouter le cours",Modifier.weight(1f)){};Tile("🎬","Vidéos",Modifier.weight(1f)){}}
  Button(quiz,Modifier.fillMaxWidth().height(54.dp),shape=RoundedCornerShape(16.dp)){Text("Je m'entraîne avec un quiz →",fontWeight=FontWeight.Bold)}
  OutlinedButton(teacher,Modifier.fillMaxWidth()){Text("Poser une question au professeur")}
  Text("Version de démonstration : le cours Pythagore est un exemple local. L'IA, l'audio et les vidéos seront connectés ensuite.",color=Color.Gray,fontSize=11.sp)
 }
}

@Composable private fun Quiz(index:Int,score:Int,done:Boolean,answer:(Boolean)->Unit,back:()->Unit,retry:()->Unit) {
 val qs=listOf("Dans quel triangle utilise-t-on Pythagore ?" to listOf("Rectangle","Équilatéral","Quelconque"),"Comment s'appelle le plus long côté ?" to listOf("La médiane","L'hypoténuse","La hauteur"),"Quelle formule est correcte ?" to listOf("a + b = c","a² + b² = c²","a² - b² = c"),"Les côtés mesurent 3 et 4. L'hypoténuse vaut…" to listOf("6","7","5"),"L'hypoténuse se trouve…" to listOf("Face à l'angle droit","À côté","N'importe où"))
 val correct=listOf(0,1,1,2,0)
 Screen {
  Row(verticalAlignment=Alignment.CenterVertically){IconButton(back){Icon(Icons.Default.ArrowBack,null)};Text("Mon petit contrôle",color=Ink,fontSize=23.sp,fontWeight=FontWeight.ExtraBold)}
  if(!done) {
   Text("Question "+(index+1)+" sur 5",color=Purple,fontWeight=FontWeight.Bold)
   LinearProgressIndicator(progress={ (index+1)/5f },Modifier.fillMaxWidth())
   Panel(Lilac){Text(qs[index].first,color=Ink,fontSize=20.sp,fontWeight=FontWeight.Bold)}
   qs[index].second.forEachIndexed{i,opt->OutlinedButton({answer(i==correct[index])},Modifier.fillMaxWidth().height(54.dp)){Text(opt,color=Ink)}}
  } else {
   val stars=score.coerceAtLeast(1)
   Text("Bravo pour tes efforts !",fontSize=25.sp,fontWeight=FontWeight.ExtraBold,color=Ink)
   Text("⭐".repeat(stars)+"☆".repeat(5-stars),fontSize=35.sp)
   Text(score.toString()+" bonnes réponses sur 5",color=Purple,fontSize=20.sp,fontWeight=FontWeight.Bold)
   Text(when(stars){5->"Leçon maîtrisée !";4->"Très bonne maîtrise !";3->"Tu progresses bien. Encore un peu d'entraînement !";else->"Chaque essai te fait avancer. Reprenons les bases ensemble !"},color=Ink)
   Button(retry,Modifier.fillMaxWidth()){Text("Recommencer")}
   OutlinedButton(back,Modifier.fillMaxWidth()){Text("Retour au cours")}
  }
 }
}

@Composable private fun Teacher(question:String,onQuestion:(String)->Unit,pet:String,back:()->Unit) {
 Screen {
  Row(verticalAlignment=Alignment.CenterVertically){IconButton(back){Icon(Icons.Default.ArrowBack,null)};Text("Mon professeur",color=Ink,fontSize=23.sp,fontWeight=FontWeight.ExtraBold)}
  Panel(Lilac){Text(pet+"  Coucou !",fontSize=25.sp);Text("Pose-moi une question sur ta leçon. Le vrai professeur IA sera connecté dans une prochaine version.",color=Ink)}
  Entry(question,onQuestion,"Écris ta question…")
  Text("La conversation IA n'est pas encore active dans cette version de démonstration.",color=Color.Gray,fontSize=12.sp)
 }
}

@Composable private fun Screen(content:@Composable ColumnScope.()->Unit) {
 Surface(Modifier.fillMaxSize(),color=Bg) {
  LazyColumn(Modifier.fillMaxSize().padding(horizontal=18.dp),contentPadding=PaddingValues(top=23.dp,bottom=30.dp)) {
   item { Column(verticalArrangement=Arrangement.spacedBy(14.dp),content=content) }
  }
 }
}
@Composable private fun Heading(title:String,sub:String) {
 Column(verticalArrangement=Arrangement.spacedBy(5.dp)){Text("EDUKO ✨",color=Purple,fontWeight=FontWeight.Black,letterSpacing=3.sp);Text(title,color=Ink,fontSize=27.sp,fontWeight=FontWeight.ExtraBold);Text(sub,color=Color.Gray)}
}
@Composable private fun Panel(color:Color,modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit) {
 Card(modifier.fillMaxWidth(),shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=color)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=content)}
}
@Composable private fun Panel(brush:Brush,content:@Composable ColumnScope.()->Unit) {
 Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(24.dp)){Column(Modifier.background(brush).padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp),content=content)}
}
@Composable private fun Entry(value:String,onValue:(String)->Unit,hint:String) {
 Row(Modifier.fillMaxWidth().background(Color.White,RoundedCornerShape(14.dp)).padding(14.dp)) {
  Box(Modifier.weight(1f)){if(value.isEmpty())Text(hint,color=Color.Gray,fontSize=14.sp);BasicTextField(value,onValue,Modifier.fillMaxWidth(),textStyle=androidx.compose.ui.text.TextStyle(color=Ink,fontSize=15.sp))}
 }
}
@Composable private fun Tile(icon:String,label:String,modifier:Modifier=Modifier,onClick:()->Unit) {
 Card(modifier.clickable(onClick=onClick),shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Color.White)){
  Column(Modifier.fillMaxWidth().padding(14.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp)){Text(icon,fontSize=28.sp);Text(label,color=Ink,fontWeight=FontWeight.SemiBold,fontSize=12.sp,textAlign=TextAlign.Center)}
 }
}
