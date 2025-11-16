 /* Copyright (c) 2025. Jericho Crosby <jericho.crosby@gmail.com> */

 package com.chalwk.model;

 public class EventMessage {
     private String channel_id;
     private String text;

     // Getters and setters
     public String getChannel_id() {
         return channel_id;
     }

     public void setChannel_id(String channel_id) {
         this.channel_id = channel_id;
     }

     public String getText() {
         return text;
     }

     public void setText(String text) {
         this.text = text;
     }
 }