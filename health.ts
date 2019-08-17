import { Component } from '@angular/core';
import { IonicPage, NavController, NavParams } from 'ionic-angular';
import { Health } from '@ionic-native/health';
import { AlertController } from 'ionic-angular';
import { LoadingController } from 'ionic-angular';
import { HomePage } from '../home/home';
import { isGeneratedFile } from '@angular/compiler/src/aot/util';
import {DummyPage} from '../dummy/dummy';

declare var samsunghealth;

/**
 * Generated class for the HealthPage page.
 *
 * See https://ionicframework.com/docs/components/#navigation for more info on
 * Ionic pages and navigation.
 */

@Component({
  selector: 'page-health',
  templateUrl: 'health.html',
})
export class HealthPage {

  loader;
  stepsToday: number = 0;
  stepsTodayString:String = "0" ; 
  totalSteps: number = 0;
  stepsThisMonth: number = 500;
  stepsLastMonth: number = 0;
  stepsLastMonth1: number = 0;
  stepsLastMonth2: number = 0;
  avgStepsThisMonth: number = 1000;
  avgStepsLastThreeMonth: number = 0;
  healthObj: Health;
  currentDate: String;
  currentDate_last: String;
  currentDate_last1: String;
  currentDate_last2: String;
  averageStepsLastMonth: number = 0;
  averageStepsLastMonth1: number = 0;
  averageStepsLastMonth2: number = 0;
  daysInLastMonth: number = 0;
  daysInLastMonth1: number = 0;
  daysInLastMonth2: number = 0;
  status: String = "None";
  source: String;
  dashboardImageName: String;
  dashboardImageurl: String = "./assets/imgs/0.png";

  constructor(public navCtrl: NavController, public navParams: NavParams, private health: Health, public alertCtrl: AlertController, public loadingCtrl: LoadingController) {
    this.healthObj = health;
    this.loader = loadingCtrl;
    var todaydate: Date = new Date();
    
    this.currentDate = this.formatDate(todaydate.getMonth(), todaydate.getFullYear());

    this.currentDate_last = this.formatDate(todaydate.getMonth() -1, todaydate.getFullYear());
    this.daysInLastMonth = this.daysInMonth(todaydate.getMonth()-1,todaydate.getFullYear());
    console.log("last month date " + this.currentDate_last);
    console.log("days in last month " + this.daysInLastMonth);

    this.currentDate_last1 = this.formatDate(todaydate.getMonth() -2, todaydate.getFullYear());
    this.daysInLastMonth1 = this.daysInMonth(todaydate.getMonth()-2,todaydate.getFullYear());
    console.log("last month second date " + this.currentDate_last1);
    console.log("days in last second month " + this.daysInLastMonth1);


    this.currentDate_last2 = this.formatDate(todaydate.getMonth() -3, todaydate.getFullYear());
    this.daysInLastMonth2 = this.daysInMonth(todaydate.getMonth()-3,todaydate.getFullYear());
    console.log("last month third date " + this.currentDate_last2);
    console.log("days in last third month " + this.daysInLastMonth2);


    this.source = localStorage.getItem("selectedDataSource");
    
    if(localStorage.getItem("selectedDataSource") == "Google Health"){
      this.authorizeGoogleFit();
    } else if(localStorage.getItem("selectedDataSource") == "Apple Health") {
      this.authorizeApple();
    } else {
      this.authorizeSamsung();
    }

    
  }

  daysInMonth(month: number, year: number){
      return new Date(year, month+1, 0).getDate();
  }

  formatDate(month, year) {
    var monthNames = [
      "January", "February", "March",
      "April", "May", "June", "July",
      "August", "September", "October",
      "November", "December"
    ];
  
    var monthIndex = month;
    var year = year;
  
    return  monthNames[monthIndex] + ', ' + year;
  }

  ionViewDidLoad() {
    console.log('ionViewDidLoad HealthPage');
  }

  authorizeSamsung(){
    this.presentLoading(); 
    samsunghealth.connect((success)=>{
      console.log('Connection success');
      var currentDate = new Date();
      currentDate.setDate(1);
      currentDate.setHours(0);
      currentDate.setMinutes(0);
      currentDate.setMilliseconds(0);
      currentDate.setSeconds(0);
      var fromDateTime = currentDate.getTime(); 
      var tillDateTime = new Date().getTime();
      // alert("From date is " + currentDate + " and mili second is " + fromDateTime);
      // alert("To date is " + new Date() + " and mili second is " + tillDateTime);
      samsunghealth.getData(fromDateTime,tillDateTime,(success)=>{
      // alert(JSON.stringify(success))
      console.log('Get data success');
      this.dismissLoading();
      this.calculateStepsFromSamsungHealth((success));
      
    },(err)=>{
      this.showAlert(JSON.stringify(err));
      console.log(JSON.stringify(err));
      this.presentLoading();
    });
    },(err)=>{
      this.showAlert(JSON.stringify(err)); 
      console.log(JSON.stringify(err));
      this.presentLoading();
    });

  }

  calculateStepsFromSamsungHealth(success){
    console.log(success);
    var i:number;
    for(i=0;i<success.length;i++){
        this.stepsThisMonth = this.stepsThisMonth + success[i]["stepCount"]
    }
    var currentDate = new Date().getDate();
    var dateOfFirstReading = new Date(success[0]["date"]);
    if(currentDate == dateOfFirstReading.getDate()){
      this.stepsToday = success[0]["stepCount"];
      this.avgStepsThisMonth = Math.round(this.stepsThisMonth/currentDate);   
    }   
  }


  authorizeApple(){
    this.authorizeGoogleFit();
  }
  
  authorizeGoogleFit(){
    this.presentLoading();
      this.health.isAvailable()
      .then((available:boolean) => {
        console.log(available);
        console.log("Health available")
        this.health.requestAuthorization([
          {
            read: ['steps'],     
          }
        ])
        .then(res => 
          {
            console.log(res)
            console.log("Authprization success")
            this.loadStepsFromGoogleFit();
          })
        .catch(e => 
          {
            console.log(e);
            console.log("Authorization failed")
            this.showAlert("Authorization failed");
            this.dismissLoading();
          }
          );

      })
      .catch(e => 
        {
          console.log(e);
          console.log("Health not available")
          this.showAlert("Health not available");
          this.dismissLoading();
        }
        );
    
  }

  loadStepsFromGoogleFit(){
    var currentMonth = new Date();
    currentMonth.setDate(1); 
    var lastThirdMonth =  currentMonth.setMonth(currentMonth.getMonth()-3);
    
    this.health.queryAggregated({
      startDate: new Date(), // three days ago
      endDate: new Date(), // now
      dataType: 'steps',
      bucket: 'day',
      filtered: true
      })  .then(res => 
      {
        console.log(res);
        this.showTodaysSteps(res);
      }
      ) 
      .catch(e => 
        {
          console.log(e);
          this.showAlert(e);
          this.dismissLoading();
        }
        );   

    this.health.queryAggregated({
      startDate: new Date(lastThirdMonth), // three days ago
      endDate: new Date(), // now
      dataType: 'steps',
      bucket: 'month',
      filtered: true
      })  .then(res => 
      {
        console.log(res);
        this.calculateSteps(res);
      }
      ) 
      .catch(e => 
        {
          console.log(e);
          this.showAlert(e);
          this.dismissLoading();
        }
        );
    
         

  }

  calculateSteps(res){
    var currentDay = new Date().getDate();
    console.log("Current day no " + currentDay)
    
    this.totalSteps = res[0]['value'] + res[1]['value'] + res[2]['value'] + res[3]['value'];
    this.avgStepsLastThreeMonth = this.totalSteps/3;

    this.stepsThisMonth = res[3]['value']; 
    this.stepsLastMonth = res[2]['value']; 
    this.stepsLastMonth1 = res[1]['value']; 
    this.stepsLastMonth2 = res[0]['value']; 

    this.avgStepsThisMonth = Math.round(this.stepsThisMonth/currentDay); 
    this.averageStepsLastMonth = Math.round(res[2]['value']/this.daysInLastMonth);
    this.averageStepsLastMonth1 = Math.round(res[1]['value']/this.daysInLastMonth1);
    this.averageStepsLastMonth2 = Math.round(res[0]['value']/this.daysInLastMonth2);

    
   
    console.log("Total steps in last three months till now " + this.totalSteps);
    console.log("Avg steps in last 3 months " + this.avgStepsLastThreeMonth);
    
    console.log("Steps this month " + this.stepsThisMonth); 
    console.log("Avg steps this  months " +  this.avgStepsThisMonth);
    
    console.log("Total steps last  months " +  res[2]['value']);
    console.log("Average steps last  months " +  this.averageStepsLastMonth);

    console.log("Total steps last second  months " +  res[1]['value']);   
    console.log("Average steps last second  months " +  this.averageStepsLastMonth1);

    console.log("Total steps last third months " +  res[0]['value']);   
    console.log("Average steps last third  months " +  this.averageStepsLastMonth2);

    this.setStatus(res);
    this.dismissLoading();
  }

  setStatus(res){
    if(this.averageStepsLastMonth2 && this.averageStepsLastMonth1>10000 && this.averageStepsLastMonth >= 10000){
      this.status = "Platinum"
    }
    else if(this.averageStepsLastMonth >= 10000 ){
      this.status = "Gold"
    } else if(this.averageStepsLastMonth >=6000){
      this.status = "Silver"
    } else if(this.averageStepsLastMonth){
      this.status = "Bronze"
    }
    this.setDashboardImage();
  }


  showTodaysSteps(res){
    this.stepsToday = res[0]['value'];
    this.stepsTodayString = this.stepsToday.toString();
    console.log("Steps today " + this.stepsToday);
  }

  showAlert(value) {
    const alert = this.alertCtrl.create({
      title: 'Message',
      subTitle: value,
      buttons: ['OK']
    });
    alert.present();
  }


  presentLoading() {
    console.log("presenting loader");
    this.loader = this.loadingCtrl.create({
      content: "Please wait...",
    });
    this.loader.present();
  }

  dismissLoading(){  
    console.log("dismissing loader"); 
      this.loader.dismiss();
  }

 
  unsync(){
    if(localStorage.getItem("selectedDataSource") == "Google Health"){
      this.unsyncFromDashboard("google");
    } else if(localStorage.getItem("selectedDataSource") == "Apple Health") {
      this.unsyncFromDashboard("apple");
    } else {
      this.unsyncFromDashboard("samsung");
    }
  }

  unsyncFromDashboard(sourceName){
    if (sourceName=="google"){
        this.presentLoading();
        navigator["health"].disconnect((success)=>{
          localStorage.setItem('selectedDataSource', 'notSynced');
          if(this.navCtrl.length() != 1){
              this.loader.present().then(()=>{      
                  this.dismissLoading();
            });
            this.navCtrl.pop(); 
          }
          else {
            this.loader.present().then(()=>{      
                  this.dismissLoading();
            });
            this.navCtrl.setRoot(HomePage);}
          },
        (err)=>{      
          this.dismissLoading();
          this.showAlert(err);
        });
      }
  else{
      this.presentLoading();
      localStorage.setItem('selectedDataSource', 'notSynced');
      this.dismissLoading();
      this.navCtrl.setRoot(HomePage);}
    }
   
 
  showMoreDetails(){
    this.navCtrl.push(DummyPage, {
      totalSteps: this.totalSteps,
      stepsToday: this.stepsToday,
      stepsThisMonth: this.stepsThisMonth,
      stepsLastMonth: this.stepsLastMonth,
      stepsLastMonth2: this.stepsLastMonth2,
      stepsLastMonth1: this.stepsLastMonth1,
      avgStepsThisMonth:this.avgStepsThisMonth,
      averageStepsLastMonth: this.averageStepsLastMonth,
      avgStepsLastThreeMonth: this.avgStepsThisMonth,
      averageStepsLastMonth1: this.averageStepsLastMonth1,
      averageStepsLastMonth2: this.averageStepsLastMonth2,
      currentDate: this.currentDate,
      currentDate_last: this.currentDate_last,
      currentDate_last1: this.currentDate_last1,
      currentDate_last2: this.currentDate_last2,
      daysInLastMonth: this.daysInLastMonth,
      daysInLastMonth1: this.daysInLastMonth1,
      daysInLastMonth2: this.daysInLastMonth2
    });
  }

  setDashboardImage(){
    if(this.avgStepsThisMonth<500){
      this.dashboardImageName = "0.png"
    } else if(this.avgStepsThisMonth <1000){
      this.dashboardImageName = "1.png"
    } else if(this.avgStepsThisMonth <1500){
      this.dashboardImageName = "2.png"
    } else if(this.avgStepsThisMonth <2000){
      this.dashboardImageName = "3.png"
    } else if(this.avgStepsThisMonth <2500){
      this.dashboardImageName = "4.png"
    } else if(this.avgStepsThisMonth <3000){
      this.dashboardImageName = "5.png"
    } else if(this.avgStepsThisMonth <3600){
      this.dashboardImageName = "6.png"
    } else if(this.avgStepsThisMonth <4200){
      this.dashboardImageName = "7.png"
    } else if(this.avgStepsThisMonth <4800){
      this.dashboardImageName = "8.png"
    } else if(this.avgStepsThisMonth <5400){
      this.dashboardImageName = "9.png"
    } else if(this.avgStepsThisMonth <6000){
      this.dashboardImageName = "10.png"
    } else if(this.avgStepsThisMonth <6800){
      this.dashboardImageName = "11.png"
    } else if(this.avgStepsThisMonth <7600){
      this.dashboardImageName = "12.png"
    } else if(this.avgStepsThisMonth <8400){
      this.dashboardImageName = "13.png"
    } else if(this.avgStepsThisMonth <9200){
      this.dashboardImageName = "14.png"
    } else if(this.avgStepsThisMonth <10000){
      this.dashboardImageName = "15.png"
    } else if(this.avgStepsThisMonth <10800){
      this.dashboardImageName = "16.png"
    } else if(this.avgStepsThisMonth <11600){
      this.dashboardImageName = "17.png"
    } else if(this.avgStepsThisMonth <12400){
      this.dashboardImageName = "18.png"
    } else if(this.avgStepsThisMonth <13200){
      this.dashboardImageName = "19.png"
    } else if(this.avgStepsThisMonth <14000){
      this.dashboardImageName = "20.png"
    } else{
      this.dashboardImageName = "21.png"
    }

    this.dashboardImageurl = "./assets/imgs/" + this.dashboardImageName;
    console.log(this.dashboardImageurl);
    
    
  }


}
