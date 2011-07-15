#!/usr/bin/env ruby
# coding : utf-8

BASE_PATH = File.expand_path(File.dirname(__FILE__))
db_path = BASE_PATH + '/../assets/RubyKaigi2011.db'

begin
File.delete(db_path)
rescue
end

require 'sequel'
DB = Sequel.sqlite(db_path)

unless DB.table_exists? :RubyKaigi2011
  DB.create_table :RubyKaigi2011 do
    primary_key :_id
    string :day
    string :room_en
    string :room_ja
    string :start
    string :end
    string :speaker_en
    string :speaker_ja
    string :title_en
    string :title_ja
    string :desc_en
    string :desc_ja
    string :lang
    string :speaker_bio_en
    string :speaker_bio_ja
    string :gravatar
  end 
end
unless DB.table_exists? :android_metadata
  DB.create_table :android_metadata do
    text :locale
  end
  DB[:android_metadata] << {:locale => 'en_US'}
end

def write_db(data)
  DB[:RubyKaigi2011] << data
end

require 'yaml'
require 'date'

yamls = Dir.glob(BASE_PATH + '/rubykaigi/db/2011/room_timetables/*.yaml').sort!

rooms_en = {'M' => 'Main Hall','S' => 'Sub Hall'}
rooms_ja = {'M' => '大ホール','S' => '小ホール'}


#for gravatar
require 'open-uri'
require 'RMagick'
GRAVATAR_SIZE = 64
GRAVATAR_PATH = "#{BASE_PATH}/../assets"
bow_path = "#{GRAVATAR_PATH}/bow_face.jpeg"
open('http://rubykaigi.org/images/bow_face.png'){|bow|
  File.open(bow_path,'w') { |f|
    f.write bow.read
  }
}
Magick::Image.read(bow_path).first.resize_to_fit(GRAVATAR_SIZE,GRAVATAR_SIZE).write(bow_path)

def process_presenters(y)
  speakers_en = ''
  speakers_ja = ''
  speakers_bio_en = ''
  speakers_bio_ja = ''
  gravatars = ''

  y.each { |pre|
    name_en = pre['name']['en']
    speakers_en = speakers_en ? name_en : " / #{name_en}"

    name_ja = pre['name']['ja']
    speakers_ja = speakers_ja ? (name_ja || name_en) : " / #{name_ja || name_en}"

    bio_en = name_en + "\n"
    bio_en << "(#{pre['affiliation']['en']})\n" if pre['affiliation']['en']
    bio_en << "#{pre['bio']['en']}" if pre['bio']['en']
    speakers_bio_en << "\n\n" unless speakers_bio_en == ''
    speakers_bio_en << bio_en

    bio_ja = ( name_ja || name_en ) + "\n"
    bio_ja << "(#{(pre['affiliation']['ja'] || pre['affiliation']['en'])})\n" if (pre['affiliation']['ja'] || pre['affiliation']['en'])
    bio_ja << "#{(pre['bio']['ja'] || pre['bio']['en'])}" if (pre['bio']['ja'] || pre['bio']['en'])
    speakers_bio_ja << "\n\n" unless speakers_bio_ja == ''
    speakers_bio_ja << bio_ja

    speakers_bio_en.gsub! "#TODO" , ""
    speakers_bio_ja.gsub! "#TODO" , ""

    if gravatar = pre['gravatar']
      g_id = gravatar[0..7] # avoid too long file name
      open("http://www.gravatar.com/avatar/#{gravatar}?s=#{GRAVATAR_SIZE}") { |g|
        File.open("#{GRAVATAR_PATH}/#{g_id}.jpeg",'w') { |f|
          f.write g.read
        }
      }
    else
      g_id = 'bow_face'
    end
    unless gravatars == ''
      #File.delete("#{gravatar_path}/#{gravatars}.jpeg") unless gravatar == 'bow_face'
      list = Magick::ImageList.new("#{GRAVATAR_PATH}/#{gravatars}.jpeg","#{GRAVATAR_PATH}/#{g_id}.jpeg")
      img = list.append(false)
      gravatars << g_id
      img.write("#{GRAVATAR_PATH}/#{gravatars}.jpeg")
    else
      gravatars = g_id
    end
  }

  return [speakers_en,speakers_ja,speakers_bio_en,speakers_bio_ja,gravatars]

end

SPECIAL_EVENT = ['Open','Break','Lunch','Transit time','Party at Ikebukuro (door open 19:30)']

def process_event(ev,day,room_en,room_ja,start,_end,parent_title)
  title_en = ev['title']['en']
  title_ja = ev['title']['ja'] || ev['title']['en']

  title_en = "[#{parent_title}]\n#{title_en}" if parent_title
  title_ja = "[#{parent_title}]\n#{title_ja}" if parent_title

  presenters = ev['presenters']
  speakers_en,speakers_ja,speakers_bio_en,speakers_bio_ja,gravatars = process_presenters(presenters) if presenters

  if abstract = ev['abstract']
    abstract_en = abstract['en']
    abstract_ja = abstract['ja'] || abstract_en
    abstract_en = abstract_ja if abstract_en == 'TBD'

    abstract_en = "" if abstract_en == "#TODO"
    abstract_ja = "" if abstract_ja == "#TODO"
  end 

  lang = ( ev['language'] || '' ).gsub('English','en').gsub('Japanese','ja')
  lang = "[%s]" % lang unless lang == ''

  special = SPECIAL_EVENT.include? title_en

  return if special && room_en == 'Sub Hall'

  data = { 
    :day => day,
    :room_en => special ? '' : room_en,
    :room_ja => special ? '' : room_ja,
    :start => start,
    :end => _end,
    :title_en => title_en,
    :title_ja => title_ja,
    :speaker_en => speakers_en,
    :speaker_ja => speakers_ja,
    :desc_en => abstract_en,
    :desc_ja => abstract_ja,
    :lang => lang,
    :speaker_bio_en => speakers_bio_en,
    :speaker_bio_ja => speakers_bio_ja,
    :gravatar => gravatars
  }   
  if sub_events = ev['sub_event_ids']
    sub_events.each { |sub|
      sub_yaml = "#{BASE_PATH}/rubykaigi/db/2011/events/#{sub}.yaml"
      sub_ev = YAML.load_file(sub_yaml)
      process_event(sub_ev,day,room_en,room_ja,start,_end,title_en)
    }
  else
    write_db(data)
  end

end

yamls.each { |yaml|
  y = YAML.load_file(yaml)

  day = y['date'].day
  room_en = rooms_en[y['room_id']]
  room_ja = rooms_ja[y['room_id']] || room_en

  y['timeslots'].each { |slots|
    event = slots['event_ids']
    event.each { |ev|
      day = slots['start'].day
      start = "%02d:%02d" % [slots['start'].hour, slots['start'].min]
      _end = "%02d:%02d" % [slots['end'].hour,slots['end'].min]

      ev_yaml = "#{BASE_PATH}/rubykaigi/db/2011/events/#{ev}.yaml" 
      ev = YAML.load_file(ev_yaml)

      process_event(ev,day,room_en,room_ja,start,_end,nil)


    } if event
  }
}

